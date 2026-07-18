# Architecture Overview

## Story Directories

- `features/login`: login story workspace.
- `features/forgot-password`: password recovery story workspace.
- `features/token-quota-management`: token quota management story workspace.
- `features/token-usage-view`: personal token usage story workspace.
- `features/favorite-script-library`: favorite script library story workspace.
- `features/forbidden-words-management`: forbidden words story workspace.
- `features/create-agent-account`: agent account creation story workspace.
- `features/ai-chat-workbench`: intelligent conversation workspace story workspace.

## Layout Rule

Each user story owns one directory under `features/`.
Inside a story directory, split code by delivery layer only when implementation starts, for example:

- `ui/`: page, component, route, and interaction code.
- `server/`: API handler, service, or persistence code.
- `docs/`: notes, contracts, and acceptance criteria.

## Story Mapping

1. Login -> `features/login`
2. Forgot password -> `features/forgot-password`
3. Token quota management -> `features/token-quota-management`
4. Personal token usage -> `features/token-usage-view`
5. Favorite script library -> `features/favorite-script-library`
6. Forbidden words management -> `features/forbidden-words-management`
7. Create agent account -> `features/create-agent-account`
8. Intelligent conversation workspace -> `features/ai-chat-workbench`