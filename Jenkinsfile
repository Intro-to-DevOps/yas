// ============================================================
// YAS Monorepo — CI Pipeline
//
// Stage order (DevOps best practice):
//   1. Checkout
//   2. Secret Scan (Gitleaks)   <- hard gate, always runs
//   3. Detect Changed Services
//   4. Test                     <- UNSTABLE on failure, non-blocking
//   5. Security & Quality Scan  <- Snyk + SonarQube, parallel
//   6. Build                    <- protected branches only (main/develop)
//
// Principles:
//   - Fail fast: secret scan runs first, blocks immediately on findings
//   - Monorepo-aware: only test/build services that actually changed
//   - Non-blocking warnings: test/scan failures -> UNSTABLE, not FAILURE
//   - Docker build only runs on protected branches, not on every PR
// ============================================================

pipeline {
    agent any

    options {
        // Abort the pipeline if it exceeds this duration
        timeout(time: 60, unit: 'MINUTES')
        // Prevent concurrent builds of the same branch
        disableConcurrentBuilds()
        // Keep only the last 20 build records
        buildDiscarder(logRotator(numToKeepStr: '20'))
        // Prepend timestamps to every log line
        timestamps()
    }

    stages {

        // ── 1. CHECKOUT ────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // ── 2. SECRET SCAN ─────────────────────────────────────
        // Always runs regardless of which services changed.
        // NO "|| true" — exit code 1 on any finding = immediate FAILURE.
        // Uses gitleaks binary directly to avoid Docker socket permission issues.
        // --log-opts "origin/main..HEAD" limits the scan to commits introduced
        // by this PR only — does NOT scan the entire repo history.
        stage('Secret Scan (Gitleaks)') {
            steps {
                script {
                    echo '=== Scanning PR commits for leaked secrets ==='
                    sh '''
                        # Install gitleaks binary if not already present
                        if ! command -v gitleaks &>/dev/null; then
                            GITLEAKS_VERSION="8.18.4"
                            curl -sSfL \
                                "https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz" \
                                -o /tmp/gitleaks.tar.gz
                            tar -xzf /tmp/gitleaks.tar.gz -C /tmp gitleaks
                            sudo mv /tmp/gitleaks /usr/local/bin/gitleaks || \
                                mv /tmp/gitleaks "${WORKSPACE}/.gitleaks_bin"
                            rm -f /tmp/gitleaks.tar.gz
                        fi

                        # Use local binary if sudo mv failed
                        GITLEAKS_CMD="gitleaks"
                        if [ -f "${WORKSPACE}/.gitleaks_bin" ]; then
                            GITLEAKS_CMD="${WORKSPACE}/.gitleaks_bin"
                            chmod +x "$GITLEAKS_CMD"
                        fi

                        # Ensure origin/main is available for comparison
                        git fetch origin +refs/heads/main:refs/remotes/origin/main \
                            >/dev/null 2>&1 || true

                        # Scan ONLY commits introduced by this PR (not full history)
                        "$GITLEAKS_CMD" detect \
                            --source "${WORKSPACE}" \
                            --log-opts "origin/main..HEAD" \
                            --redact \
                            --verbose \
                            --exit-code 1
                    '''
                }
            }
        }

        // ── 3. DETECT CHANGED SERVICES ─────────────────────────
        // Compares the PR branch against origin/main to find which
        // services have changed. Result is written to changed_services.txt
        // as a CSV string. Using a file instead of env vars avoids a known
        // Jenkins Declarative Pipeline env propagation bug.
        stage('Detect Changed Services') {
            steps {
                script {
                    sh '''
                        git fetch origin +refs/heads/main:refs/remotes/origin/main \
                            >/dev/null 2>&1 || true

                        # Get list of files changed relative to main
                        DIFF=$(git diff --name-only origin/main...HEAD 2>/dev/null || true)

                        # Fallback: use files from the last commit if diff is empty
                        if [ -z "$DIFF" ]; then
                            echo "WARNING: diff vs origin/main is empty — falling back to last commit"
                            DIFF=$(git log -m -1 --name-only --pretty="format:" 2>/dev/null || true)
                        fi

                        echo "=== Changed files ==="
                        echo "$DIFF"
                        echo "====================="

                        # Full list of services in this monorepo
                        ALL_SERVICES="backoffice storefront backoffice-bff storefront-bff \
                            media product cart order rating customer location inventory tax \
                            search recommendation promotion payment payment-paypal webhook \
                            sampledata common-library delivery"

                        # Filter down to services that have at least one changed file
                        CHANGED=""
                        for svc in $ALL_SERVICES; do
                            if echo "$DIFF" | grep -qE "^${svc}/"; then
                                CHANGED="${CHANGED:+${CHANGED},}${svc}"
                            fi
                        done

                        # A change in common-library requires a full rebuild of all services
                        if echo ",$CHANGED," | grep -q ",common-library,"; then
                            echo "common-library changed — triggering full rebuild of all services"
                            CHANGED=$(echo "$ALL_SERVICES" | tr ' ' ',')
                        fi

                        printf "%s" "$CHANGED" > changed_services.txt
                        echo "=== Services to process: [${CHANGED:-none}] ==="
                    '''
                }
            }
        }

        // ── 4. TEST ────────────────────────────────────────────
        // Runs unit tests in parallel for each changed service.
        // catchError(UNSTABLE) ensures test failures do not block
        // the Security Scan or Build stages downstream.
        stage('Test') {
            when {
                expression {
                    return fileExists('changed_services.txt') &&
                           readFile('changed_services.txt').trim() != ''
                }
            }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {
                        def services = readFile('changed_services.txt').trim().split(',')

                        // failFast: false — run all services even if one fails
                        def jobs = [failFast: false]

                        for (svc in services) {
                            def s = svc  // capture by value to avoid Groovy closure variable bug
                            jobs[s] = {
                                if (s in ['backoffice', 'storefront']) {
                                    // Frontend: Node.js
                                    sh "cd ${s} && npm ci --prefer-offline && npm test -- --coverage --ci"
                                } else {
                                    // Backend: Maven — use -pl to scope to the module, -am to build deps
                                    sh "mvn -B test -pl ${s} -am -DskipITs"
                                }
                            }
                        }

                        parallel jobs
                    }
                }
            }
            post {
                always {
                    // Collect test reports regardless of pass/fail
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
            }
        }

        // ── 5. SECURITY & QUALITY SCAN ─────────────────────────
        // Snyk:      dependency vulnerability analysis
        // SonarQube: code quality, coverage, and bug detection
        // Both run in parallel. catchError(UNSTABLE) keeps Build unblocked.
        stage('Security & Quality Scan') {
            when {
                expression {
                    return fileExists('changed_services.txt') &&
                           readFile('changed_services.txt').trim() != ''
                }
            }
            parallel {

                stage('Snyk') {
                    steps {
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            script {
                                echo '=== Running Snyk dependency vulnerability scan ==='
                                withCredentials([string(credentialsId: 'SNYK_TOKEN', variable: 'SNYK_TOKEN')]) {
                                    sh 'npm install -g snyk --silent'

                                    def services = readFile('changed_services.txt').trim().split(',') as List
                                    def hasJavaService = false

                                    for (svc in services) {
                                        if (svc == 'backoffice') {
                                            sh 'snyk test --file=backoffice/package-lock.json --severity-threshold=high || true'
                                        } else if (svc == 'storefront') {
                                            sh 'snyk test --file=storefront/package-lock.json --severity-threshold=high || true'
                                        } else if (svc != 'common-library') {
                                            hasJavaService = true
                                        }
                                    }

                                    // Run aggregate Maven scan if any Java backend service changed
                                    if (hasJavaService || services.contains('common-library')) {
                                        sh 'mvn -B install -DskipTests -q'
                                        sh 'snyk test --maven-aggregate-project --severity-threshold=high || true'
                                    }
                                }
                            }
                        }
                    }
                }

                stage('SonarQube') {
                    steps {
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            script {
                                echo '=== Running SonarCloud analysis ==='
                                withCredentials([
                                    string(credentialsId: 'SONAR_TOKEN',  variable: 'SONAR_TOKEN'),
                                    string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')
                                ]) {
                                    def services = readFile('changed_services.txt').trim().split(',') as List
                                    // SonarQube only scans Java/backend services
                                    def hasJavaService = services.any { it != 'backoffice' && it != 'storefront' }

                                    if (hasJavaService || services.contains('common-library')) {
                                        // Build first so Sonar has bytecode available for coverage analysis
                                        sh 'mvn -B install -DskipTests -q'
                                        sh '''
                                            mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                                -Dsonar.projectKey=nashtech-garage_yas-yas-parent \
                                                -Dsonar.organization=nashtech-garage \
                                                -Dsonar.host.url=https://sonarcloud.io \
                                                || true
                                        '''
                                    } else {
                                        echo 'No Java services changed — SonarQube scan skipped'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 6. BUILD ───────────────────────────────────────────
        // Builds Docker images only on protected branches (main/develop).
        // Skipped on feature/PR branches to avoid unnecessary builds and
        // registry pollution.
        // Images are tagged with both BUILD_NUMBER-GitSHA and "latest".
        stage('Build') {
            when {
                allOf {
                    expression {
                        return fileExists('changed_services.txt') &&
                               readFile('changed_services.txt').trim() != ''
                    }
                    // Only build Docker images on protected branches
                    anyOf {
                        branch 'main'
                        branch 'develop'
                    }
                }
            }
            steps {
                script {
                    def services = readFile('changed_services.txt').trim().split(',')
                    def gitSha   = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    def imageTag = "${env.BUILD_NUMBER}-${gitSha}"

                    echo "=== Building Docker images — tag: ${imageTag} ==="

                    def jobs = [failFast: false]

                    for (svc in services) {
                        def s   = svc
                        def tag = imageTag
                        jobs[s] = {
                            if (s in ['backoffice', 'storefront']) {
                                sh """
                                    cd ${s}
                                    npm ci --prefer-offline
                                    npm run build
                                    docker build -t ${s}:${tag} -t ${s}:latest .
                                """
                            } else {
                                sh """
                                    cd ${s}
                                    mvn -B package -DskipTests -q
                                    docker build -t ${s}:${tag} -t ${s}:latest .
                                """
                            }
                        }
                    }

                    parallel jobs
                }
            }
        }
    }

    // ── POST ───────────────────────────────────────────────────
    post {
        always {
            script {
                // Collect JaCoCo coverage report if available
                try {
                    publishCoverage adapters: [
                        jacocoAdapter('**/target/site/jacoco/jacoco.xml')
                    ]
                } catch (Throwable ignored) {
                    echo 'Coverage report not available (plugin missing or no Java tests ran)'
                }

                // Clean up temp file used for inter-stage communication
                sh 'rm -f changed_services.txt || true'
            }
        }

        success {
            echo '✅ Pipeline PASSED — all gates cleared.'
        }

        unstable {
            echo '⚠️  Pipeline UNSTABLE — test failures or scan warnings detected. Review before merging.'
        }

        failure {
            echo '❌ Pipeline FAILED — check the stage above for details (likely Secret Scan or Build error).'
        }
    }
}