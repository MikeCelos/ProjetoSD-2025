JAVA = java
JAVAC = javac
SRC = $(shell find src -name "*.java")
OUT = target/classes

compile:
	@mvn -q clean package || (mkdir -p $(OUT) && $(JAVAC) -d $(OUT) $(SRC))
	@echo "Compilação concluída."

barrels:
	@echo "A iniciar Barrel 1 (porto 1099)..."
	@$(JAVA) -cp $(OUT) pt.uc.sd.googol.barrel.IndexStorageBarrel 1099 &
	sleep 1
	@echo "A iniciar Barrel 2 (porto 1100)..."
	@$(JAVA) -cp $(OUT) pt.uc.sd.googol.barrel.IndexStorageBarrel 1100 &
	sleep 1
	@echo "Barrels iniciados em background."

gateway:
	@echo "A iniciar Gateway (porto 2000)..."
	@$(JAVA) -cp $(OUT) pt.uc.sd.googol.gateway.Gateway


downloader:
	@echo "A iniciar Downloader..."
	@$(JAVA) -cp $(OUT) pt.uc.sd.googol.downloader.Downloader

client:
	@echo "A iniciar Cliente..."
	@$(JAVA) -cp $(OUT) pt.uc.sd.googol.client.GoogolClient

all: compile barrels gateway client

clean:
	@rm -rf $(OUT) target
	@echo "Diretórios limpos."

	