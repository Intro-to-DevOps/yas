pipeline {
    agent any

    tools {
        jdk 'JDK_25'
        maven 'maven'
        nodejs 'NodeJS'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare Git') {
            steps {
                script {
                    sh """
                        git fetch origin main:refs/remotes/origin/main
                        git branch -a
                    """
                }
            }
        }

        stage('Detect Changed Services') {
            steps {
                script {
                    def currentBranch = sh(
                        script: "git rev-parse --abbrev-ref HEAD",
                        returnStdout: true
                    ).trim()

                    def headCommit = sh(
                        script: "git rev-parse HEAD",
                        returnStdout: true
                    ).trim()

                    def mainCommit = sh(
                        script: "git rev-parse origin/main || true",
                        returnStdout: true
                    ).trim()

                    echo "========== FETCH CHECK =========="
                    sh "git branch -a"
                    sh "git remote -v"

                    echo "========== DIFF FILES =========="

                    def changedFilesRaw = sh(
                        script: "git diff --name-only origin/main..HEAD || true",
                        returnStdout: true
                    ).trim()

                    def changedFiles = changedFilesRaw ? changedFilesRaw.split("\\n") : []

                    def allServices = [
                        "backoffice", "storefront",
                        "backoffice-bff", "storefront-bff",
                        "media", "product", "cart", "order", "rating",
                        "customer", "location", "inventory", "tax",
                        "search", "recommendation", "promotion",
                        "payment", "payment-paypal", "webhook", "sampledata",
                        "common-library", "delivery"
                    ]

                    def changed = []

                    for (file in changedFiles) {
                        echo "Checking file: ${file}"
                        for (svc in allServices) {
                            if (file.startsWith("${svc}/") || file.contains("/${svc}/")) {
                                echo "→ Matched service: ${svc}"
                                changed.add(svc)
                            }
                        }
                    }

                    changed = changed.unique()

                    if (changed.contains("common-library")) {
                        echo "Common library changed → rebuild all services"
                        changed = allServices
                    }

                    def result = changed.join(",")

                    env.CHANGED_SERVICES = result.toString()

                    if (!env.CHANGED_SERVICES?.trim()) {
                        env.CHANGED_SERVICES = ""
                    }
                    echo "========== FINAL RESULT =========="
                    echo "Changed services: ${CHANGED_SERVICES}"
                }
            }
        }

        // =========================
        // PREPARE DEPENDENCIES
        // =========================
        stage('Prepare Dependencies') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def rawServices = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(',').collect { it.trim() } : []
                    def services = rawServices.unique()
                    def hasMaven = services.any { !(it in ["backoffice", "storefront"]) }
                    
                    if (hasMaven) {
                        echo "=========================================================="
                        echo "[PREPARE] INSTALLING MAVEN DEPENDENCIES TO LOCAL REPO"
                        echo "=========================================================="
                        sh "mvn clean install -DskipTests"
                    }
                }
            }
        }

        // =========================
        // TEST PHASE
        // =========================
        stage('Test') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    // 1. Get the list of services, trim whitespace, and remove DUPLICATES (unique)
                    def rawServices = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(',').collect { it.trim() } : []
                    def services = rawServices.unique() // This completely solves the issue of running multiple times

                    // 2. Categorize Java and NodeJS (Frontend) services
                    def javaServices = services.findAll { !(it in ['backoffice', 'storefront']) && it != '' }
                    def nodeServices = services.findAll { it in ['backoffice', 'storefront'] }

                    def jobs = [:]

                    // 3. Process Java Services: Combine into a single command!
                    if (!javaServices.isEmpty()) {
                        def plArgs = javaServices.join(',') // Example: "product,cart"
                        jobs['Java Services Tests'] = {
                            sh "chmod +x mvnw"
                            // Bring back the -am flag. Since we run in a single command, there are no race conditions or ${revision} errors
                            sh "./mvnw -B test jacoco:report -pl ${plArgs} -am -DskipITs -Dmaven.test.failure.ignore=true"

                            // Aggregate coverage reports from all modules using **
                            jacoco(
                                execPattern: '**/target/jacoco.exec',
                                classPattern: '**/target/classes',
                                sourcePattern: '**/src/main/java',
                                minimumInstructionCoverage: '70', maximumInstructionCoverage: '70',
                                minimumLineCoverage: '70', maximumLineCoverage: '70',
                                minimumBranchCoverage: '70', maximumBranchCoverage: '70',
                                changeBuildStatus: true
                            )
                            if (currentBuild.result == 'FAILURE' || currentBuild.result == 'UNSTABLE') {
                                error("Test coverage below 70%")
                            }
                        }
                    }

                    // 4. Process Frontend (Node): Keep running in parallel as they are completely independent
                    for (nodeSvc in nodeServices) {
                        def svcName = nodeSvc // Assign to a local variable to avoid Groovy loop scope issues
                        jobs[svcName] = {
                            sh """
                                cd ${svcName}
                                npm ci
                                npm test -- --coverage
                            """
                        }
                    }

                    // 5. Execute in parallel
                    if (jobs.size() > 0) {
                        parallel jobs
                    } else {
                        echo "No services to test."
                    }
                }
            }
        }

        // =========================
        // SECURITY SCAN
        // =========================
        stage('Security Scan') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def rawServices = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(',').collect { it.trim() } : []
                    def services = rawServices.unique()
                    def securityJobs = [:]

                    securityJobs['Gitleaks'] = {
                        catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                            sh """
                                echo "=========================================================="
                                echo "[SECURITY] STARTING GITLEAKS SCAN (origin/main..HEAD)"
                                echo "=========================================================="
                                curl -sL https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz | tar xz
                                ./gitleaks detect --log-opts="origin/main..HEAD" --verbose
                            """
                        }
                    }

                    for (svc in services) {
                        def currentSvc = svc

                        securityJobs["SonarCloud-${currentSvc}"] = {
                            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                                    if (currentSvc in ["backoffice", "storefront"]) {
                                        sh """
                                            echo "=========================================================="
                                            echo "[SECURITY] STARTING SONARCLOUD SCAN (JS/NPM): ${currentSvc}"
                                            echo "=========================================================="
                                            cd ${currentSvc}
                                            sonar-scanner -Dsonar.projectKey=intro-to-devops_yas-${currentSvc} -Dsonar.organization=intro-to-devops -Dsonar.sources=. -Dsonar.host.url=https://sonarcloud.io -Dsonar.token=\$SONAR_TOKEN
                                        """
                                    } else {
                                        sh """
                                            echo "=========================================================="
                                            echo "[SECURITY] STARTING SONARCLOUD SCAN (MAVEN): ${currentSvc}"
                                            echo "=========================================================="
                                            cd ${currentSvc}
                                            mvn sonar:sonar -Dsonar.projectKey=intro-to-devops_yas-${currentSvc} -Dsonar.organization=intro-to-devops -Dsonar.host.url=https://sonarcloud.io -Dsonar.token=\$SONAR_TOKEN
                                        """
                                    }
                                }
                            }
                        }

                        securityJobs["Snyk-${currentSvc}"] = {
                            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                                withCredentials([string(credentialsId: 'SNYK_TOKEN', variable: 'SNYK_TOKEN')]) {
                                    if (currentSvc in ["backoffice", "storefront"]) {
                                        sh """
                                            echo "=========================================================="
                                            echo "[SECURITY] STARTING SNYK VULNERABILITY SCAN (NPM): ${currentSvc}"
                                            echo "=========================================================="
                                            cd ${currentSvc}
                                            npx snyk test
                                        """
                                    } else {
                                        sh """
                                            echo "=========================================================="
                                            echo "[SECURITY] STARTING SNYK VULNERABILITY SCAN (MAVEN): ${currentSvc}"
                                            echo "=========================================================="
                                            chmod +x mvnw || true
                                            chmod +x ${currentSvc}/mvnw || true
                                            npx snyk test --file=${currentSvc}/pom.xml --command=mvn
                                        """
                                    }
                                }
                            }
                        }
                    }

                    // Execute security scans in parallel
                    parallel securityJobs
                }
            }
        }

        // =========================
        // BUILD PHASE
        // =========================
        stage('Build') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def rawServices = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(',').collect { it.trim() } : []
                    def services = rawServices.unique()

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
                                    chmod +x mvnw
                                    ./mvnw package -DskipTests
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
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

            publishCoverage adapters: [
                jacocoAdapter('**/target/site/jacoco/jacoco.xml')
            ]

            script {
                // Aggregate all HTML reports from services into a single directory for display
                sh """
                    mkdir -p jacoco-html-reports
                    
                    # Create a root index.html to contain links to all services
                    echo '<html><head><title>JaCoCo Reports</title><style>body{font-family:Arial,sans-serif; margin:40px;} li{margin:10px 0;} a{text-decoration:none; color:#0366d6;} a:hover{text-decoration:underline;}</style></head><body>' > jacoco-html-reports/index.html
                    echo '<h2>JaCoCo HTML Coverage Reports</h2><ul>' >> jacoco-html-reports/index.html
                    
                    # Iterate through all generated jacoco directories
                    for dir in */target/site/jacoco; do
                        if [ -d "\$dir" ]; then
                            # Get the service name (e.g., cart, product...)
                            svc=\$(dirname \$(dirname \$(dirname \$dir)))
                            
                            # Copy all HTML files of that service into the common directory
                            mkdir -p "jacoco-html-reports/\$svc"
                            cp -r "\$dir/"* "jacoco-html-reports/\$svc/"
                            
                            # Add a link to the root index.html
                            echo "<li><a href='\${svc}/index.html'>Coverage Report for <b>\${svc}</b></a></li>" >> jacoco-html-reports/index.html
                        fi
                    done
                    
                    echo '</ul></body></html>' >> jacoco-html-reports/index.html
                """
            }

            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'jacoco-html-reports',
                reportFiles: 'index.html',
                reportName: 'JaCoCo HTML Report'
            ])
        }
    }
}