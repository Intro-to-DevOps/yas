pipeline {
    agent any

    stages {
        stage('Khởi tạo') {
            steps {
                echo 'Bắt đầu chạy CI Pipeline...'
                echo 'Đã kết nối thành công từ GitHub sang server Jenkins của thầy!'
            }
        }

        stage('Test (Skeleton)') {
            steps {
                echo 'Đang ở Phase Test...'
                echo 'Tuần sau sẽ cấu hình lệnh chạy test thực tế (VD: mvn test) tại đây.'
                // sh './mvnw clean test'
            }
        }

        stage('Build (Skeleton)') {
            steps {
                echo 'Đang ở Phase Build...'
                echo 'Tuần sau sẽ cấu hình lệnh build thực tế (VD: mvn package) tại đây.'
                // sh './mvnw clean package -DskipTests'
            }
        }
    }

    post {
        always {
            echo 'Pipeline đã chạy xong!'
        }
        success {
            echo 'Trạng thái: THÀNH CÔNG (Pass)'
        }
        failure {
            echo 'Trạng thái: THẤT BẠI (Fail)'
        }
    }
}