package com.nextplugins.economy;

import com.google.common.base.Stopwatch;
import com.henryfabio.minecraft.inventoryapi.manager.InventoryManager;
import com.henryfabio.sqlprovider.connector.SQLConnector;
import com.henryfabio.sqlprovider.executor.SQLExecutor;
import com.nextplugins.economy.backup.BackupManager;
import com.nextplugins.economy.command.CommandRegistry;
import com.nextplugins.economy.configuration.FeatureValue;
import com.nextplugins.economy.configuration.registry.ConfigurationRegistry;
import com.nextplugins.economy.dao.SQLProvider;
import com.nextplugins.economy.dao.repository.AccountRepository;
import com.nextplugins.economy.group.GroupWrapperManager;
import com.nextplugins.economy.listener.ListenerRegistry;
import com.nextplugins.economy.model.storage.AccountStorage;
import com.nextplugins.economy.placeholder.Placeholders;
import com.nextplugins.economy.ranking.RankingBootstrap;
import com.nextplugins.economy.ranking.RankingChatBody;
import com.nextplugins.economy.ranking.RankingStorage;
import com.nextplugins.economy.util.title.InternalAPIMapping;
import com.nextplugins.economy.util.title.InternalTitleAPI;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Getter
public final class NextEconomy extends JavaPlugin {

    private InternalTitleAPI internalTitleAPI;

    private SQLConnector sqlConnector;
    private SQLExecutor sqlExecutor;

    private File configFile;

    private FileConfiguration configuration;

    private AccountRepository accountRepository;

    private AccountStorage accountStorage;
    private RankingStorage rankingStorage;

    private BackupManager backupManager;
    private GroupWrapperManager groupWrapperManager;

    private RankingBootstrap rankingBootstrap;
    private RankingChatBody rankingChatBody;

    @Override
    public void onLoad() {
        final File dataFolder = getDataFolder();

        dataFolder.mkdir();

        saveResource("configuration.yml", false);

        this.internalTitleAPI = InternalAPIMapping.create();

        this.configFile = new File(dataFolder, "configuration.yml");
        this.configuration = YamlConfiguration.loadConfiguration(configFile);

        this.sqlConnector = SQLProvider.of(this).setup(null);
        this.sqlExecutor = new SQLExecutor(sqlConnector);

        this.accountRepository = new AccountRepository(sqlExecutor);
        this.accountStorage = new AccountStorage(accountRepository);

        this.backupManager = new BackupManager();
    }

    @Override
    public void onEnable() {
        getLogger().info("Iniciando carregamento do plugin...");

        final Stopwatch loadTime = Stopwatch.createStarted();

        this.groupWrapperManager = new GroupWrapperManager();
        this.rankingChatBody = new RankingChatBody();

        this.rankingStorage = new RankingStorage(accountRepository, groupWrapperManager, rankingChatBody);

        this.rankingBootstrap = new RankingBootstrap(this);

        accountStorage.init(getConfig().getBoolean("plugin.configuration.nick-save-method", true));

        InventoryManager.enable(this);

        EconomyServiceInjector.inject();

        ConfigurationRegistry.of(this).register();
        CommandRegistry.of(this).register();

        Bukkit.getScheduler()
                .runTaskLater(
                        this,
                        () -> {
                            Placeholders.register();
                            ListenerRegistry.of(this).register();

                            groupWrapperManager.init();
                            purgeBackups();
                        },
                        150L);

        loadTime.stop();
        getLogger().log(Level.INFO, "Plugin inicializado com sucesso. ({0})", loadTime);
    }

    @Override
    public void onDisable() {
        accountStorage.flushData();
        rankingBootstrap.shutdown();

        createBackup();
    }

    private void purgeBackups() {
        val path = new File("plugins/NextEconomy/backups");
        if (!path.exists()) return;

        val list = path.listFiles();
        if (list == null) return;

        for (File file : list) {
            try {
                val basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

                val fileTime = basicFileAttributes.creationTime();
                if (fileTime.toMillis() > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)) continue;

                if (file.delete()) {
                    getLogger().info("O backup " + file.getName() + " foi apagado por ser muito antigo.");
                } else {
                    getLogger().warning("Não foi possível apagar o backup " + file.getName() + ".");
                }
            } catch (Exception exception) {
                getLogger().warning("Ocorreu um erro ao tentar apagar o backup " + file.getName() + ".");
            }
        }
    }

    private void createBackup() {
        if (FeatureValue.get(FeatureValue::autoBackup)) {
            CompletableFuture.completedFuture(backupManager.createBackup(null, null, accountRepository, false, false))
                    .join(); // freeze thread
        }
    }

    @Override
    public void saveConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Não foi possível salvar o arquivo " + configFile, ex);
        }
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        return configuration;
    }

    public static NextEconomy getInstance() {
        return getPlugin(NextEconomy.class);
    }
}
