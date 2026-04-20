pipeline {
    agent any

  

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
                expression { env.CHANGED_SERVICES?.trim() }
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
                                docker run --rm -v \${WORKSPACE}:/workspace -w /workspace/${currentSvc} node:20 bash -c 'npm ci && npm test -- --coverage'
                                """
                            } else {
                                sh """
                                docker run --rm -v \${WORKSPACE}:/workspace -v maven-repo:/root/.m2 -w /workspace/${currentSvc} maven:3.9.6-eclipse-temurin-21 mvn test
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
                            sh 'docker run --rm -v ${WORKSPACE}:/work -w /work zricethezav/gitleaks:v8.18.4 detect --source="." --verbose --no-git'
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
                                        docker run --rm -v \${WORKSPACE}:/usr/src -w /usr/src/${currentSvc} \
                                          -e SONAR_HOST_URL=\$SONAR_HOST_URL \
                                          -e SONAR_AUTH_TOKEN=\$SONAR_AUTH_TOKEN \
                                          sonarsource/sonar-scanner-cli \
                                          sonar-scanner -Dsonar.projectKey=nashtech-garage_yas_${currentSvc} -Dsonar.sources=.
                                        """
                                    }
                                } else {
                                    withSonarQubeEnv('sonarcloud') {
                                        sh """
                                        docker run --rm -v \${WORKSPACE}:/workspace -v maven-repo:/root/.m2 -w /workspace/${currentSvc} \
                                          -e SONAR_HOST_URL=\$SONAR_HOST_URL \
                                          -e SONAR_AUTH_TOKEN=\$SONAR_AUTH_TOKEN \
                                          maven:3.9.6-eclipse-temurin-21 mvn sonar:sonar
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
                                        docker run --rm -v \${WORKSPACE}:/app -w /app/${currentSvc} -e SNYK_TOKEN=\$SNYK_TOKEN snyk/snyk:node snyk test
                                        """
                                    } else {
                                        sh """
                                        docker run --rm -v \${WORKSPACE}:/app -v maven-repo:/root/.m2 -w /app/${currentSvc} -e SNYK_TOKEN=\$SNYK_TOKEN snyk/snyk:maven-3-jdk-21 snyk test --all-projects
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
                    def services = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(",") : []

                    def jobs = [:]

                    for (svc in services) {
                        def currentSvc = svc
                        jobs[currentSvc] = {
                            if (currentSvc in ["backoffice", "storefront"]) {
                                sh """
                                docker run --rm -v \${WORKSPACE}:/workspace -w /workspace/${currentSvc} node:20 npm run build
                                cd ${currentSvc}
                                docker build -t ${currentSvc}:latest .
                                """
                            } else {
                                sh """
                                docker run --rm -v \${WORKSPACE}:/workspace -v maven-repo:/root/.m2 -w /workspace/${currentSvc} maven:3.9.6-eclipse-temurin-21 mvn package -DskipTests
                                cd ${currentSvc}
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