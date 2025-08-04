from fastapi import FastAPI
from app.router.recommendation_router import router as recommendation_router

app = FastAPI()
app.include_router(recommendation_router, prefix="/api")

@app.get("/")
def root():
    return {"message": "AI Server is running"}