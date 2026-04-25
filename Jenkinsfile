pipeline {
    agent any

    tools {
        jdk 'JDK_25'
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
                        expression { env.CHANGED_SERVICES?.trim() }
                    }
                    steps {
                        script {
                            // 1. Lấy danh sách service, loại bỏ khoảng trắng và loại bỏ các phần tử TRÙNG LẶP (unique)
                            def rawServices = env.CHANGED_SERVICES?.trim() ? env.CHANGED_SERVICES.split(',').collect{ it.trim() } : []
                            def services = rawServices.unique() // Dòng này giải quyết triệt để lỗi chạy 4 lần

                            // 2. Phân loại Java và Nodejs (Frontend)
                            def javaServices = services.findAll { !(it in ['backoffice', 'storefront']) && it != '' }
                            def nodeServices = services.findAll { it in ['backoffice', 'storefront'] }

                            def jobs = [:]

                            // 3. Xử lý Java Services: Gom vào 1 lệnh duy nhất!
                            if (!javaServices.isEmpty()) {
                                def plArgs = javaServices.join(',') // Ví dụ: "product,cart"
                                jobs['Java Services Tests'] = {
                                    // Mang cờ -am trở lại. Vì chạy trong 1 lệnh, sẽ không có đụng độ (Race Condition) và không lỗi ${revision}
                                    sh "./mvnw -B test jacoco:report -pl ${plArgs} -am -DskipITs"

                                    // Gom báo cáo coverage của tất cả module bằng dấu **
                                    jacoco(
                                        execPattern: '**/target/jacoco.exec',
                                        classPattern: '**/target/classes',
                                        sourcePattern: '**/src/main/java',
                                        minimumInstructionCoverage: '70',
                                        minimumLineCoverage: '70',
                                        minimumBranchCoverage: '70',
                                        changeBuildStatus: true
                                    )
                                }
                            }

                            // 4. Xử lý Frontend (Node): Vẫn cho chạy song song vì chúng độc lập hoàn toàn
                            for (nodeSvc in nodeServices) {
                                def svcName = nodeSvc // Gán vào biến local để tránh lỗi vòng lặp của Groovy
                                jobs[svcName] = {
                                    sh """
                                    cd ${svcName}
                                    npm ci
                                    npm test -- --coverage
                                    """
                                }
                            }

                            // 5. Chạy parallel
                            if (jobs.size() > 0) {
                                parallel jobs
                            } else {
                                echo "Không có service nào cần test."
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
                echo "Placeholder for SonarQube, Snyk, Gitleaks"
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
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

            publishCoverage adapters: [
                jacocoAdapter('**/target/site/jacoco/jacoco.xml')
            ]
        }
    }
}