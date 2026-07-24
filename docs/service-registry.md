# Service Registry

This registry records known feature services for final integration and Nginx routing. It is informational and does not change how feature owners run their local code.

| Feature | Path | Backend Port | API Prefix | Status | Notes |
| --- | --- | ---: | --- | --- | --- |
| Login | `features/login` | TBD | `/api/auth/**` | README only | Needs owner confirmation. |
| Forgot password | `features/forgot-password` | TBD | `/api/password/**` | README only | Needs owner confirmation. |
| Token quota management | `features/token-quota-management` | TBD | `/api/token-quota/**` | Has server code | Port and final API prefix need confirmation. |
| Token usage view | `features/token-usage-view` | TBD | `/api/token-usage/**` | README only | Needs owner confirmation. |
| Favorite script library | `features/favorite-script-library` | TBD | `/api/scripts/**` | README only | Needs owner confirmation. |
| Forbidden words management | `features/forbidden-words-management` | 8080 | `/api/forbidden-words/**` | Has Spring Boot server | Port found in `server/src/main/resources/application.yml`. |
| Create agent account | `features/create-agent-account` | TBD | `/api/agents/**` | README only | Needs owner confirmation. |
| AI chat workbench | `features/ai-chat-workbench` | 8081 | `/api/chat/**` | Has Spring Boot server | Port found in `server/src/main/resources/application.yml`. |

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