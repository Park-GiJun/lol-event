rootProject.name = "lol-event-backend"

// eureka-server 와 api-gateway 는 지웠다.
//   eureka-server : 등록할 서비스가 main-service 하나뿐인 디스커버리 + Config Server 였다.
//                   설정은 main-service 의 application-prd.yml 과, 배포 스크립트가
//                   /config 로 마운트하는 호스트 설정 파일로 옮겼다.
//   api-gateway   : 라우팅 다섯 개와 CORS 가 전부였다. JWT 필터는 통과만 하는 빈 껍데기였고,
//                   그 앞단에 호스트 nginx 가 이미 있어 한 겹이 더 끼어 있었다.
//                   CORS 는 main-service 의 WebConfig 로, 라우팅은 nginx 로 옮겼다.
// 되살릴 일이 있으면 git history 에서 꺼내면 된다 (7832682 이전).
include("common", "main-service")
