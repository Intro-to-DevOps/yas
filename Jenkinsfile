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
                    def changedFiles = sh(
                        script: "git diff --name-only origin/main...HEAD",
                        returnStdout: true
                    ).trim().split("\n")

                    def allServices = [
                        // frontend
                        "backoffice",
                        "storefront",

                        // bff
                        "backoffice-bff",
                        "storefront-bff",

                        // backend
                        "media",
                        "product",
                        "cart",
                        "order",
                        "rating",
                        "customer",
                        "location",
                        "inventory",
                        "tax",
                        "search",
                        "recommendation",
                        "promotion",
                        "payment",
                        "payment-paypal",
                        "webhook",
                        "sampledata",

                        // shared
                        "common-library",
                        "delivery"
                    ]

                    def changed = []

                    for (file in changedFiles) {
                        for (svc in allServices) {
                            if (file.startsWith("${svc}/")) {
                                changed.add(svc)
                            }
                        }
                    }

                    changed = changed.unique()

                    if (changed.contains("common-library")) {
                        echo "Common library changed → rebuild all services"
                        changed = allServices
                    }

                    env.CHANGED_SERVICES = changed.join(",")
                    echo "Changed services: ${env.CHANGED_SERVICES}"
                }
            }
        }

        // =========================
        // TEST PHASE
        // =========================
        stage('Test') {
            when {
                expression { env.CHANGED_SERVICES != "" }
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
                expression { env.CHANGED_SERVICES != "" }
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
                expression { env.CHANGED_SERVICES != "" }
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
            // Java test reports
            junit '**/target/surefire-reports/*.xml'

            // Coverage
            publishCoverage adapters: [
                jacocoAdapter('**/target/site/jacoco/jacoco.xml')
            ]
        }
    }
}