pipeline {
    agent any

    environment {
        CHANGED_SERVICES = ""
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Detect Changed Services') {
            steps {
                script {
                    def changedServicesStr = sh(
                        script: '''
                            git fetch origin +refs/heads/main:refs/remotes/origin/main >/dev/null 2>&1 || true

                            DIFF=$(git diff --name-only origin/main...HEAD 2>/dev/null || true)
                            if [ -z "$DIFF" ]; then
                                echo "WARNING: origin/main diff empty, falling back to last commit files" >&2
                                DIFF=$(git log -m -1 --name-only --pretty="format:" 2>/dev/null || true)
                            fi

                            echo "--- GIT DIFF OUTPUT ---" >&2
                            echo "$DIFF" >&2

                            SERVICES="backoffice storefront backoffice-bff storefront-bff media product cart order rating customer location inventory tax search recommendation promotion payment payment-paypal webhook sampledata common-library delivery"

                            CHANGED=""
                            for svc in $SERVICES; do
                                if echo "$DIFF" | grep -qE "^${svc}/"; then
                                    if [ -z "$CHANGED" ]; then
                                        CHANGED="$svc"
                                    else
                                        CHANGED="$CHANGED,$svc"
                                    fi
                                fi
                            done

                            # If common-library changed, rebuild all services
                            if echo ",$CHANGED," | grep -q ",common-library,"; then
                                echo "common-library changed, rebuilding all services" >&2
                                CHANGED=$(echo "$SERVICES" | tr " " ",")
                            fi

                            echo "$CHANGED"
                        ''',
                        returnStdout: true
                    ).trim()

                    env.CHANGED_SERVICES = changedServicesStr
                    echo "Changed services: ${env.CHANGED_SERVICES}"
                }
            }
        }

        // =========================
        // TEST PHASE
        // =========================
        stage('Test') {
            when {
                expression { env.CHANGED_SERVICES != null && env.CHANGED_SERVICES != "" }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(",")

                    def jobs = [:]

                    for (svc in services) {
                        jobs[svc] = {
                            if (svc in ["backoffice", "storefront"]) {
                                sh """
                                cd ${svc}
                                npm ci
                                npm test -- --coverage
                                """
                            } else {
                                sh """
                                cd ${svc}
                                mvn test
                                """
                            }
                        }
                    }

                    parallel jobs
                }
            }
        }

        // =========================
        // SECURITY SCAN
        // =========================
        stage('Security Scan') {
            when {
                expression { env.CHANGED_SERVICES != null && env.CHANGED_SERVICES != "" }
            }
            parallel {
                stage('Gitleaks') {
                    steps {
                        script {
                            echo "Running Gitleaks Scan..."
                            sh 'docker run --rm -v ${WORKSPACE}:/path zricethezav/gitleaks:latest detect --source /path -v'
                        }
                    }
                }

                stage('Snyk Scan') {
                    steps {
                        script {
                            echo "Running Snyk Scan..."
                            withCredentials([string(credentialsId: 'SNYK_TOKEN', variable: 'SNYK_TOKEN')]) {
                                sh 'npm install -g snyk'
                                
                                def services = env.CHANGED_SERVICES.split(",")
                                def hasJava = false
                                
                                for (svc in services) {
                                    if (svc == "backoffice") {
                                        sh 'snyk test --file=backoffice/package-lock.json --severity-threshold=high || true'
                                    } else if (svc == "storefront") {
                                        sh 'snyk test --file=storefront/package-lock.json --severity-threshold=high || true'
                                    } else if (svc != "common-library") {
                                        hasJava = true
                                    }
                                }
                                
                                if (hasJava || services.contains("common-library")) {
                                    sh 'mvn install -DskipTests'
                                    sh 'snyk test --maven-aggregate-project --severity-threshold=high || true'
                                }
                            }
                        }
                    }
                }

                stage('SonarQube') {
                    steps {
                        script {
                            echo "Running SonarCloud Scan..."
                            withCredentials([
                                string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN'),
                                string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')
                            ]) {
                                def services = env.CHANGED_SERVICES.split(",")
                                def hasJava = services.any { it != "backoffice" && it != "storefront" }
                                
                                if (hasJava || services.contains("common-library")) {
                                    sh 'mvn install -DskipTests'
                                    sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=nashtech-garage_yas-yas-parent'
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================
        // BUILD PHASE
        // =========================
        stage('Build') {
            when {
                expression { env.CHANGED_SERVICES != null && env.CHANGED_SERVICES != "" }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(",")

                    def jobs = [:]

                    for (svc in services) {
                        jobs[svc] = {
                            if (svc in ["backoffice", "storefront"]) {
                                sh """
                                cd ${svc}
                                npm run build
                                docker build -t ${svc}:latest .
                                """
                            } else {
                                sh """
                                cd ${svc}
                                mvn package -DskipTests
                                docker build -t ${svc}:latest .
                                """
                            }
                        }
                    }

                    parallel jobs
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    // Java test reports
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                } catch (Throwable t) {
                    echo "Ignoring error in JUnit reports: ${t.getMessage()}"
                }
                
                try {
                    // Coverage
                    publishCoverage adapters: [
                        jacocoAdapter('**/target/site/jacoco/jacoco.xml')
                    ]
                } catch (Throwable t) {
                    echo "Ignoring error in Jacoco/Coverage: ${t.getMessage()} - PENDING ADMIN INSTALLING CODE COVERAGE API PLUGIN."
                }
            }
        }
    }
}