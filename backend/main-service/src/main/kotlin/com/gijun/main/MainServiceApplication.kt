package com.gijun.main

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * @EnableDiscoveryClient 는 뗐다. Eureka 에 등록할 서비스가 이것 하나뿐이라
 * 디스커버리가 찾아 줄 대상이 자기 자신밖에 없었다. 자세한 배경은 application-prd.yml 참고.
 */
@SpringBootApplication
@EnableScheduling
class MainServiceApplication

fun main(args: Array<String>) {
    runApplication<MainServiceApplication>(*args)
}
