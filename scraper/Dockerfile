FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
WORKDIR /app
COPY scraper/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY scraper/app ./app
CMD ["python", "-m", "app.main"]
