package com.nextplugins.economy.group;

import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.group.impl.VaultGroupWrapper;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
public final class GroupWrapperManager {

    private GroupWrapper wrapper;

    public void init() {
        wrapper = new VaultGroupWrapper();
        wrapper.setup();

        NextEconomy.getInstance()
                .getLogger()
                .info("[Grupos] Integrado com sucesso com o plugin '"
                        + wrapper.getClass().getSimpleName() + "'");
    }

    public Group getGroup(String playerName) {
        try {
            return wrapper.getGroup(playerName);
        } catch (Throwable ignored) {
            return new Group();
        }
    }
}
