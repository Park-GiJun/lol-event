rootProject.name = "lol-event-backend"

// eureka-server 와 api-gateway 는 빌드에서 뺐다.
//   eureka-server : 등록할 서비스가 main-service 하나뿐인 디스커버리 + Config Server 였다.
//                   설정은 main-service 의 application-prd.yml 과 환경변수로 옮겼다.
//   api-gateway   : 라우팅 다섯 개와 CORS 가 전부였다. JWT 필터는 통과만 하는 빈 껍데기였고,
//                   라우팅은 프론트를 서빙하는 nginx(frontend/nginx.conf)가 대신한다.
//                   CORS 는 main-service 의 WebConfig 로 옮겼다.
// 소스는 backend/eureka-server, backend/api-gateway 에 남아 있다. 되돌릴 일이 없다고
// 판단되면 그때 디렉터리째 지우면 된다.
include("common", "main-service")
