# 1. Run jenkins server

Chạy lệnh sau
```bash
docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:lts
```

Mở localhost:8080, config theo suggest của nó, vào được UI

# 2. Cài jdk 25

1. Truy cập link: [JDK 25](https://www.oracle.com/java/technologies/javase/jdk25-archive-downloads.html)
2. Tìm bản **Linux x64 Compressed Archive**, down về
3. Copy file zip vào docker. Đứng tại vị trí đã tải bản zip về, mở cmd, chạy lệnh
```bash
docker cp jdk-25.0.2_linux-x64_bin.tar.gz <tên_container>:/var/jenkins_home/
``` 
*jdk-25.0.2_linux-x64_bin.tar.gz* là tên file zip download về, thường down về tên nó chỉ tới **.tar**, nhưng lúc copy vào docker thì thêm luôn **.gz** 

4. Giải nén trong docker
```bash
docker exec -u root <tên_container> bash -c "cd /var/jenkins_home && tar -xvf jdk-25.0.2_linux-x64_bin.tar.gz && mv jdk-25.0.2 jdk25 && chown -R jenkins:jenkins jdk25"
```

5. Kiểm tra jdk
```bash
docker exec <tên_container> /var/jenkins_home/jdk25/bin/java -version
```

Nó sẽ ra jdk version

# 3. Cấu hình trong jenkins server
Vào Manage Jenkins > Tools > JDK installations:
- Add JDK
- Name: JDK_25
- JAVA_HOME: /var/jenkins_home/jdk25
- Save
