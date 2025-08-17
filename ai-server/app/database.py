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
# connect_args는 SQLite를 사용할 때만 필요합니다.
engine = create_engine(
    DATABASE_URL, 
    connect_args={"check_same_thread": False} if "sqlite" in DATABASE_URL else {}
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
