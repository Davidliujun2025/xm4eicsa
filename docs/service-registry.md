# Service Registry

This registry records known feature services for final integration and Nginx routing. It is informational and does not change how feature owners run their local code.

| Feature | Path | Backend Port | API Prefix | Status | Notes |
| --- | --- | ---: | --- | --- | --- |
| Login | `features/login` | 8080 | `/api/v1/auth/**`, `/api/v1/public/**` | Has Spring Boot server | Port and prefixes verified from controllers and `application.yml`. |
| Forgot password | `features/forgot-password` | 8082 | `/api/password/**`, `/api/auth/password-reset/**` | Has Spring Boot server | Port updated to avoid conflicts. |
| Token quota management | `features/token-quota-management` | TBD | `/api/token-quota/**` | Has server code | Port and final API prefix need confirmation. |
| Token usage view | `features/token-usage-view` | TBD | `/api/token-usage/**` | README only | Needs owner confirmation. |
| Favorite script library | `features/favorite-script-library` | TBD | `/api/scripts/**` | README only | Needs owner confirmation. |
| Forbidden words management | `features/forbidden-words-management` | 8084 | `/api/forbidden-words/**`, `/api/chat-audit/**` | Has Spring Boot server | Port updated to avoid conflicts. |
| Create agent account | `features/create-agent-account` | 8083 | `/api/admin/customer-service-users/**` | Has Spring Boot server | Port updated to avoid conflicts. |
| AI chat workbench | `features/ai-chat-workbench` | 8085 | `/api/v1/conversations/**`, `/api/v1/favorites/**`, `/api/v1/tokens/**` | Has Spring Boot server | Port updated to avoid conflicts. |

## Update Rule

When a feature owner finishes a backend, record:

```text
Feature:
Backend port:
API prefix:
Startup command:
Build command:
Database tables:
Health check URL:
```