from sqlalchemy import Column, Integer, BigInteger, String, Text, ForeignKey
from sqlalchemy.orm import relationship
from .database import Base

class Project(Base):
    # Spring Boot의 ProjectRecruitment 엔티티에 매핑
    __tablename__ = "project_recruitment"

    project_id = Column(Integer, primary_key=True, index=True)
    title = Column(String(255), nullable=False)
    description = Column(Text)

    # ProjectRecruitment와 ProjectTechStack 간의 1:N 관계 설정
    tech_stacks = relationship("ProjectTechStack", back_populates="project")

class ProjectTechStack(Base):
    # Spring Boot의 ProjectTechStack 엔티티에 매핑
    __tablename__ = "project_tech_stack"

    id = Column(Integer, primary_key=True, index=True)
    tech_stack_id = Column(Integer, ForeignKey("tech_stack.tech_stack_id"), nullable=False)
    project_id = Column(Integer, ForeignKey("project_recruitment.project_id"))
    recruit_count = Column(Integer, nullable=False)

    project = relationship("Project", back_populates="tech_stacks")
    tech_stack = relationship("TechStack")

class TechStack(Base):
    # Spring Boot의 TechStack 엔티티에 매핑
    __tablename__ = "tech_stack"
    
    tech_stack_id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50), nullable=False)
