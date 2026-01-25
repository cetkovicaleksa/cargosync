import logging

logger = logging.getLogger(__name__)

def main():
    try:
        import locust
        logger.info(f"Locust version: {locust.__version__}")
    except ImportError:
        logger.error("Locust is NOT installed")

if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO, 
        format="%(levelname)s: %(message)s"
    )
    main()
