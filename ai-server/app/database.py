import os
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv

# .env 파일에서 환경변수를 로드합니다.
load_dotenv()

# .env 파일에 설정된 DATABASE_URL을 가져옵니다.
# 만약 이 값이 없다면, 기본적으로 프로젝트 루트에 sqlite.db 라는 파일을 생성합니다.
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./sqlite.db")

# SQLAlchemy 엔진을 생성합니다.
# Spring Boot 백엔드 서버와 데이터베이스를 공유하므로 커넥션 풀을 제한합니다.
if "postgresql" in DATABASE_URL:
    engine = create_engine(
        DATABASE_URL,
        pool_size=4,           # Spring Boot는 6개, AI 서버는 4개로 분할
        max_overflow=2,        # 최대 추가 커넥션
        pool_timeout=30,       # 커넥션 대기 시간
        pool_recycle=1800,     # 커넥션 재활용 (30분)
        pool_pre_ping=True,    # 커넥션 유효성 검사
        echo=False             # SQL 로그 비활성화 (성능 향상)
    )
else:
    # SQLite용 설정
    engine = create_engine(
        DATABASE_URL, 
        connect_args={"check_same_thread": False}
    )

# 데이터베이스 세션을 생성하는 클래스입니다.
# 이 클래스의 인스턴스를 통해 DB와 통신하게 됩니다.
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# ORM 모델의 기본이 되는 Base 클래스입니다.
# 나중에 DB 테이블과 매핑될 모델 클래스들이 이 Base를 상속받게 됩니다.
Base = declarative_base()

# FastAPI 의존성 주입용 함수
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
