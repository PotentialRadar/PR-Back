from sqlalchemy import Column, Integer, BigInteger, String, Text, ForeignKey, DateTime, Numeric, Boolean
from sqlalchemy.orm import relationship
from .database import Base

class Project(Base):
    # Spring Boot의 ProjectRecruitment 엔티티에 매핑
    __tablename__ = "project_recruitment"

    project_id = Column(BigInteger, primary_key=True, index=True)
    title = Column(String(255), nullable=False)
    description = Column(String(255), nullable=False)
    start_date = Column(DateTime)
    end_date = Column(DateTime)
    recruit_count = Column(Integer, nullable=False)
    recruit_deadline = Column(DateTime)
    view_count = Column(Integer)
    team_leader_id = Column(BigInteger, ForeignKey("users.user_id"))
    status = Column(String(255), nullable=False)
    created_at = Column(DateTime)
    updated_at = Column(DateTime)

    # ProjectRecruitment와 ProjectTechStack 간의 1:N 관계 설정
    tech_stacks = relationship("ProjectTechStack", back_populates="project")

class ProjectTechStack(Base):
    # Spring Boot의 ProjectTechStack 엔티티에 매핑
    __tablename__ = "project_tech_stack"

    id = Column(BigInteger, primary_key=True, index=True)
    tech_stack_id = Column(BigInteger, ForeignKey("tech_stack.tech_stack_id"), nullable=False)
    project_id = Column(BigInteger, ForeignKey("project_recruitment.project_id"), nullable=False)
    recruit_count = Column(Integer, nullable=False)

    project = relationship("Project", back_populates="tech_stacks")
    tech_stack = relationship("TechStack")

class TechStack(Base):
    # Spring Boot의 TechStack 엔티티에 매핑
    __tablename__ = "tech_stack"
    
    tech_stack_id = Column(BigInteger, primary_key=True, index=True)
    name = Column(String(255), nullable=False)

class User(Base):
    # Spring Boot의 User 엔티티에 매핑
    __tablename__ = "users"
    
    user_id = Column(BigInteger, primary_key=True, index=True)
    email = Column(String, nullable=False, unique=True)  # DB에서는 citext
    nickname = Column(String, nullable=False, unique=True)  # DB에서는 citext
    password = Column(String(255))
    profile_image = Column(Text)
    provider = Column(String(255), nullable=False)
    provider_user_id = Column(String(255))
    created_at = Column(DateTime)
    updated_at = Column(DateTime)
    
    # User와 UserTechStack 관계
    tech_stacks = relationship("UserTechStack", back_populates="user")
    # User와 UserProfile 관계
    profile = relationship("UserProfile", back_populates="user", uselist=False)

class UserProfile(Base):
    # Spring Boot의 UserProfile 엔티티에 매핑
    __tablename__ = "user_profile"
    
    user_id = Column(BigInteger, ForeignKey("users.user_id"), primary_key=True, index=True)
    bio = Column(String(255))
    job_title = Column(String(50))
    phone = Column(String(255))
    github_url = Column(String(255))
    linkedin_url = Column(String(255))
    website_url = Column(String(255))
    reputation_score = Column(Numeric(5, 2))
    review_count = Column(Integer)
    experience_range = Column(String(255))
    is_contact_open = Column(Boolean, default=False)
    is_portfolio_open = Column(Boolean, default=False)
    is_search_open = Column(Boolean, default=False)
    tech_part_id = Column(BigInteger)
    
    # UserProfile과 User 관계
    user = relationship("User", back_populates="profile")

class UserTechStack(Base):
    # Spring Boot의 UserTechStack 엔티티에 매핑
    __tablename__ = "user_tech_stack"
    
    user_tech_stack_id = Column(BigInteger, primary_key=True, index=True)
    user_id = Column(BigInteger, ForeignKey("users.user_id"), nullable=False)
    stack_id = Column(BigInteger, ForeignKey("tech_stack.tech_stack_id"), nullable=False)
    skill_level = Column(Integer, nullable=False)
    
    # 관계 설정
    user = relationship("User", back_populates="tech_stacks")
    stack = relationship("TechStack")
