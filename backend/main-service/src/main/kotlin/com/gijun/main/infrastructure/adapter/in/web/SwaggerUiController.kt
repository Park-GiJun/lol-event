package com.gijun.main.infrastructure.adapter.`in`.web

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * springdoc Swagger UI가 Spring Boot 4.0 auto-configuration에서 로드되지 않는 문제를 해결하기 위해
 * CDN 기반 Swagger UI를 직접 서빙합니다.
 * API 스펙은 springdoc이 생성하는 /api-docs 엔드포인트를 참조합니다.
 */
@Controller
class SwaggerUiController {

    @GetMapping("/swagger-ui.html", "/swagger-ui/index.html", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun swaggerUi(): String = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>LoL 내전 이벤트 API</title>
            <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
            <style>
                body { margin: 0; background: #fafafa; }
                .topbar { display: none !important; }
            </style>
        </head>
        <body>
            <div id="swagger-ui"></div>
            <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
            <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-standalone-preset.js"></script>
            <script>
                window.onload = () => {
                    SwaggerUIBundle({
                        url: '/api-docs',
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                            SwaggerUIBundle.presets.apis,
                            SwaggerUIStandalonePreset,
                        ],
                        plugins: [SwaggerUIBundle.plugins.DownloadUrl],
                        layout: 'StandaloneLayout',
                        tryItOutEnabled: true,
                        filter: true,
                    });
                };
            </script>
        </body>
        </html>
    """.trimIndent()
}
