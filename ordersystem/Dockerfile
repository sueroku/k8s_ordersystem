
FROM openjdk:11 as stage1

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .

RUN chmod 777 gradlew
RUN ./gradlew bootjar

FROM openjdk:11
WORKDIR /app
COPY --from=stage1 /app/build/libs/*.jar app.jar

# CMD 또느 ENTRYPOINT 를 통해 컨테이너를 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

# docker 컨테이너 내에서 밖의 전체 host를 지칭하는 도메인 : host.docker.internal // 빌드전이라면 yml에서  url: jdbc:mariadb://host.docker.internal:3306/ordersystem_docker
# 빌드 후라서 실행 중 주입
# docker run -d -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/ordersystem_docker spring_test:latest
# docker run -d -p 8080:8080 -e SPRING_DATASOURCE_USERNAME

# 도커 컨테이너 실행 시에 볼륨을 설정할 때에는 -v 옵션 사용
# docker run -d -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/ordersystem -v C:\Users\Playdata\Desktop\tmp_logs(host경로):/app/logs(도커 컨테이너 내부 경로(linux위에 sp~~test 코드 로그)) spring_test:latest
# docker run -d -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/ordersystem -v C:\Users\Playdata\Desktop\tmp_logs:/app/logs ordersystem:latest