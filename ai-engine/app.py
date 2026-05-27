import fastapi
from fastapi import FastAPI

add=fastapi.FastAPI()

@add.post("/")
async def root():
    return {"message": "Hello World"}
