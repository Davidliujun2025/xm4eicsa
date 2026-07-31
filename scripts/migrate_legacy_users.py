"""Migrate legacy XM4 accounts into the integrated sys_user table.

The legacy source tables are left untouched. Plaintext legacy passwords are
converted to BCrypt before insertion into sys_user.
"""

from __future__ import annotations

import os

import bcrypt
import pymysql


def is_bcrypt(value: str | None) -> bool:
    return bool(value and value.startswith(("$2a$", "$2b$", "$2y$")))


def password_hash(primary: str | None, fallback: str | None) -> str:
    if is_bcrypt(primary):
        return primary  # type: ignore[return-value]
    if is_bcrypt(fallback):
        return fallback  # type: ignore[return-value]
    plaintext = primary or fallback
    if not plaintext:
        raise ValueError("Legacy account has no usable password")
    return bcrypt.hashpw(plaintext.encode("utf-8"), bcrypt.gensalt(12)).decode("ascii")


def normalized_status(value: str | None) -> str:
    return "ENABLED" if (value or "").upper() in {"ACTIVE", "ENABLED"} else "DISABLED"


def main() -> None:
    connection = pymysql.connect(
        host=os.environ.get("REMOTE_DB_HOST", "127.0.0.1"),
        port=int(os.environ.get("REMOTE_DB_PORT", "13306")),
        user=os.environ.get("REMOTE_DB_USER", "root"),
        password=os.environ["REMOTE_DB_PASSWORD"],
        database=os.environ.get("REMOTE_DB_NAME", "xm4"),
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )

    inserted_customers = 0
    inserted_admins = 0
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT * FROM customer_service_user ORDER BY id")
            for legacy in cursor.fetchall():
                username = (
                    legacy.get("display_name")
                    or legacy.get("name")
                    or legacy.get("account")
                    or legacy.get("phone")
                    or legacy.get("email")
                )
                if not username:
                    continue
                encoded_password = password_hash(
                    legacy.get("password_hash"),
                    legacy.get("password"),
                )
                cursor.execute(
                    """
                    INSERT INTO sys_user
                        (id, username, phone, email, password_hash, password,
                         status, role_type, last_login_at, created_at, updated_at)
                    VALUES
                        (%s, %s, %s, %s, %s, NULL, %s,
                         'CUSTOMER_SERVICE', %s,
                         COALESCE(%s, CURRENT_TIMESTAMP(6)),
                         COALESCE(%s, COALESCE(%s, CURRENT_TIMESTAMP(6))))
                    ON DUPLICATE KEY UPDATE
                        username = VALUES(username),
                        phone = VALUES(phone),
                        email = VALUES(email),
                        password_hash = VALUES(password_hash),
                        password = NULL,
                        status = VALUES(status),
                        last_login_at = VALUES(last_login_at),
                        updated_at = VALUES(updated_at)
                    """,
                    (
                        legacy["id"],
                        username,
                        legacy.get("phone"),
                        legacy.get("email"),
                        encoded_password,
                        normalized_status(legacy.get("status")),
                        legacy.get("last_login_at"),
                        legacy.get("created_at"),
                        legacy.get("updated_at"),
                        legacy.get("created_at"),
                    ),
                )
                inserted_customers += 1

            cursor.execute("SELECT * FROM agent_accounts ORDER BY user_id")
            for legacy in cursor.fetchall():
                username = legacy.get("name") or legacy.get("phone") or legacy.get("email")
                if not username:
                    continue
                cursor.execute(
                    """
                    INSERT INTO sys_user
                        (username, phone, email, password_hash, password,
                         status, role_type, created_at, updated_at)
                    VALUES
                        (%s, %s, %s, %s, NULL, %s, 'ADMIN',
                         COALESCE(%s, CURRENT_TIMESTAMP(6)),
                         COALESCE(%s, COALESCE(%s, CURRENT_TIMESTAMP(6))))
                    ON DUPLICATE KEY UPDATE
                        username = VALUES(username),
                        password_hash = VALUES(password_hash),
                        password = NULL,
                        status = VALUES(status),
                        role_type = 'ADMIN',
                        updated_at = VALUES(updated_at)
                    """,
                    (
                        username,
                        legacy.get("phone"),
                        legacy.get("email"),
                        password_hash(legacy.get("password_hash"), None),
                        normalized_status(legacy.get("status")),
                        legacy.get("created_at"),
                        legacy.get("updated_at"),
                        legacy.get("created_at"),
                    ),
                )
                inserted_admins += 1

            cursor.execute(
                """
                SELECT role_type, status, COUNT(*) AS account_count
                FROM sys_user
                GROUP BY role_type, status
                ORDER BY role_type, status
                """
            )
            groups = cursor.fetchall()
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    print(f"LEGACY_CUSTOMERS_PROCESSED={inserted_customers}")
    print(f"LEGACY_ADMINS_PROCESSED={inserted_admins}")
    for group in groups:
        print(
            "SYS_USER_GROUP="
            f"{group['role_type']}|{group['status']}|{group['account_count']}"
        )


if __name__ == "__main__":
    main()
