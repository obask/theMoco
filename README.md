# theMoco

### start microservice
`STRAVA_ACCESS_TOKEN={oauth_token} sbt run`

### usage
`curl http://localhost:8080/most_contested/{activity_id}`

### tests
`sbt test`

### properties

+ runs on 8080 port locally
+ returns 404 when activity doesn't have any segments
+ returns 500 on failed request
