pipeline {
    agent any

    tools {
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
        // TEST PHASE
        // =========================
        stage('Test') {
            when {
                // Tạm thời vô hiệu hóa để Tuần 2,3 test Security nhanh hơn
                expression { false }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(",") : []

                    def jobs = [:]

                    for (svc in services) {
                        def currentSvc = svc
                        jobs[currentSvc] = {
                            if (currentSvc in ["backoffice", "storefront"]) {
                                sh """
                                cd ${currentSvc}
                                npm ci
                                npm test -- --coverage
                                """
                            } else {
                                sh """
                                cd ${currentSvc}
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
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(",") : []
                    def securityJobs = [:]

                    // Gitleaks Scan for global repository (Week 2 task)
                    securityJobs['Gitleaks'] = {
                        catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                            // Tải trực tiếp file chạy Gitleaks (phiên bản 8.18.4) thay vì gọi qua Docker để né lỗi Permission denied
                            sh """
                            wget -qO- https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz | tar xz
                            ./gitleaks detect --log-opts="origin/main..HEAD" --verbose
                            """
                        }
                    }

                    for (svc in services) {
                        def currentSvc = svc

                        // SonarCloud Scan per service (Week 3 task)
                        securityJobs["SonarCloud-${currentSvc}"] = {
                            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                                if (currentSvc in ["backoffice", "storefront"]) {
                                    withSonarQubeEnv('sonarcloud') {
                                        sh """
                                        cd ${currentSvc}
                                        sonar-scanner -Dsonar.projectKey=nashtech-garage_yas_${currentSvc} -Dsonar.sources=.
                                        """
                                    }
                                } else {
                                    withSonarQubeEnv('sonarcloud') {
                                        sh """
                                        cd ${currentSvc}
                                        mvn sonar:sonar
                                        """
                                    }
                                }
                            }
                        }

                        // Snyk Scan per service (Week 3 task)
                        securityJobs["Snyk-${currentSvc}"] = {
                            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                                    if (currentSvc in ["backoffice", "storefront"]) {
                                        sh """
                                        cd ${currentSvc}
                                        npx snyk test
                                        """
                                    } else {
                                        sh """
                                        cd ${currentSvc}
                                        npx snyk test --all-projects
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
                // Tạm thời vô hiệu hóa để tránh build docker tốn thời gian
                expression { false }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(",") : []

                    def jobs = [:]

                    for (svc in services) {
                        def currentSvc = svc
                        jobs[currentSvc] = {
                            if (currentSvc in ["backoffice", "storefront"]) {
                                sh """
                                cd ${currentSvc}
                                npm run build
                                docker build -t ${currentSvc}:latest .
                                """
                            } else {
                                sh """
                                cd ${currentSvc}
                                mvn package -DskipTests
                                docker build -t ${currentSvc}:latest .
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
        }
    }
}