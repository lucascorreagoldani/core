package com.realmmc.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Classe utilitária para criação e envio de mensagens interativas no chat usando a Adventure API.
 * Projetada para PaperSpigot 1.21+ e Java 21.
 *
 * <p>Recursos:</p>
 * <ul>
 *   <li>Componentes de texto com eventos de hover e clique</li>
 *   <li>Mensagens na action bar</li>
 *   <li>Mensagens de título</li>
 *   <li>Botões e links customizáveis</li>
 *   <li>Suporte a placeholders e mensagens multi-linha</li>
 * </ul>
 *
 * <p>Exemplo de uso em outros plugins:</p>
 * <pre>
 * {@code
 * import com.realmmc.core.utils.jsonUtils;
 * import net.kyori.adventure.text.Component;
 * import net.kyori.adventure.text.format.NamedTextColor;
 *
 * Player player = Bukkit.getPlayer("exemplo");
 * Component mensagem = jsonUtils.createInteractiveText(
 *     "Clique aqui!",
 *     "Isto é um hover",
 *     jsonUtils.HoverType.SHOW_TEXT,
 *     "/comandoExemplo",
 *     jsonUtils.ClickType.RUN_COMMAND
 * );
 * jsonUtils.sendMessage(player, mensagem);
 * }
 * </pre>
 *
 * @author Lucas Corrêa
 */

public final class jsonUtils {

    // Supported hover event types
    public enum HoverType {
        SHOW_TEXT,
        SHOW_ITEM,
        SHOW_ENTITY
    }

    // Supported click event types
    public enum ClickType {
        RUN_COMMAND,
        SUGGEST_COMMAND,
        OPEN_URL,
        COPY_TO_CLIPBOARD
    }

    private jsonUtils() {
        throw new AssertionError("§cEsta classe não deve ser instanciada.");
    }

    /**
     * Cria um componente de texto simples.
     *
     * @param text O conteúdo do texto.
     * @return Um componente de texto.
     */
    public static Component createText(String text) {
        return Component.text(text);
    }

    /**
     * Cria um componente de texto colorido.
     *
     * @param text  O conteúdo do texto.
     * @param color A cor do texto.
     * @return Um componente de texto colorido.
     */
    public static Component createText(String text, NamedTextColor color) {
        return Component.text(text).color(color);
    }

    /**
     * Cria um componente de texto com evento de hover.
     *
     * @param text      O conteúdo do texto.
     * @param hoverText O texto exibido ao passar o mouse.
     * @param hoverType O tipo de evento de hover.
     * @return Um componente de texto com hover.
     */
    public static Component createHoverText(String text, String hoverText, HoverType hoverType) {
        Component base = Component.text(text);
        if (hoverText != null) {
            switch (hoverType) {
                case SHOW_ITEM -> base = base.hoverEvent(HoverEvent.showText(Component.text(hoverText)));
                case SHOW_ENTITY -> base = base.hoverEvent(HoverEvent.showText(Component.text(hoverText)));
                default -> base = base.hoverEvent(HoverEvent.showText(Component.text(hoverText)));
            }
        }
        return base;
    }

    /**
     * Cria um componente de texto com evento de clique.
     *
     * @param text       O conteúdo do texto.
     * @param clickValue O valor do evento de clique.
     * @param clickType  O tipo de evento de clique.
     * @return Um componente de texto com clique.
     */
    public static Component createClickText(String text, String clickValue, ClickType clickType) {
        Component base = Component.text(text);
        if (clickValue != null && clickType != null) {
            base = base.clickEvent(convertClickType(clickType, clickValue));
        }
        return base;
    }

    /**
     * Cria um componente de texto interativo com hover e clique.
     *
     * @param text       O conteúdo do texto.
     * @param hoverText  O texto exibido ao passar o mouse.
     * @param hoverType  O tipo de evento de hover.
     * @param clickValue O valor do evento de clique.
     * @param clickType  O tipo de evento de clique.
     * @return Um componente de texto interativo.
     */
    public static Component createInteractiveText(String text, String hoverText, HoverType hoverType, String clickValue, ClickType clickType) {
        Component base = Component.text(text);
        if (hoverText != null && hoverType != null) {
            base = base.hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        }
        if (clickValue != null && clickType != null) {
            base = base.clickEvent(convertClickType(clickType, clickValue));
        }
        return base;
    }

    /**
     * Envia uma mensagem para o jogador.
     *
     * @param player    Jogador que receberá a mensagem.
     * @param component Componente da mensagem.
     */
    public static void sendMessage(Player player, Component component) {
        player.sendMessage(component);
    }

    /**
     * Envia uma mensagem na action bar para o jogador.
     *
     * @param player    Jogador que receberá a mensagem.
     * @param component Componente da action bar.
     */
    public static void sendActionBar(Player player, Component component) {
        player.sendActionBar(component);
    }

    /**
     * Envia uma mensagem de título para o jogador.
     *
     * @param player   Jogador que receberá o título.
     * @param title    Componente do título.
     * @param subtitle Componente do subtítulo.
     * @param fadeIn   Duração do fade-in (em ticks).
     * @param stay     Duração de exibição (em ticks).
     * @param fadeOut  Duração do fade-out (em ticks).
     */
    public static void sendTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle, net.kyori.adventure.title.Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
    }

    /**
     * Converte um ClickType para um ClickEvent da Adventure API.
     *
     * @param type  Tipo de clique.
     * @param value Valor do evento de clique.
     * @return ClickEvent da Adventure API.
     */
    private static ClickEvent convertClickType(ClickType type, String value) {
        return switch (type) {
            case SUGGEST_COMMAND -> ClickEvent.suggestCommand(value);
            case OPEN_URL -> ClickEvent.openUrl(value);
            case COPY_TO_CLIPBOARD -> ClickEvent.copyToClipboard(value);
            default -> ClickEvent.runCommand(value);
        };
    }
}