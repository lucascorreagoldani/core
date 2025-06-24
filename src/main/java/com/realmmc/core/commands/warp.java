package com.realmmc.core.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.realmmc.core.utils.jsonUtils;
import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class warp implements CommandExecutor, Listener {

    private final FileConfiguration config;
    private final Plugin plugin;
    private final Set<UUID> teleportingPlayers = new HashSet<>();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private final Map<UUID, Location> teleportStartLocations = new HashMap<>();
    private final Map<UUID, String> warpNames = new HashMap<>();
    private final int teleportDelay = 5;

    private static final TextComponent MENSAGEM_CANCELAMENTO = new TextComponent(ChatColor.RED + "O seu teleporte foi cancelado pois você se moveu!");
    private static final String MENSAGEM_ESPERA = ChatColor.YELLOW + "Teleportando para %s em %d segundos, não se mova!";
    private static final String WARP_NAO_ENCONTRADA = ChatColor.RED + "A warp %s não foi encontrada!";
    private static final String MUNDO_WARP_INVALIDO = ChatColor.RED + "Mundo da warp não foi encontrado!";
    private static final String PERMISSAO_OUTROS = ChatColor.RED + "Apenas Moderador ou superiores podem executar esse comando!";
    private static final String JOGADOR_OFFLINE = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " está offline no momento!";
    private static final String JOGADOR_INEXISTENTE = ChatColor.RED + "O jogador %s nunca entrou no servidor!";
    private static final String AUTO_TELEPORTE = ChatColor.RED + "Você não pode teleportar a si mesmo para uma warp!";
    private static final String SUCESSO_TELEPORTE = ChatColor.GREEN + "Teleportado para a warp %s!";
    private static final String SUCESSO_TELEPORTE_OUTRO = ChatColor.GREEN + "Você teleportou o jogador " + "%s" + ChatColor.GREEN + " para a warp %s!";
    private static final String NOTIFICACAO_TELEPORTE = ChatColor.YELLOW + "Você foi teleportado para a warp %s por " + "%s" + ChatColor.YELLOW + ".";
    private static final String COMANDO_CONSOLE = ChatColor.RED + "Apenas jogadores podem usar este comando!";
    private static final String EM_ANDAMENTO = ChatColor.RED + "Você já tem um teleporte em andamento!";
    private static final String TEMPO_ESPERA = ChatColor.RED + "Aguarde %d segundos para se teleportar novamente!";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /warp <warp>:[jogador]";
    private static final String LISTA_WARPS = ChatColor.GOLD + "Warps disponíveis:";
    private static final String SINTAXE_TELEPORTE_OUTRO = ChatColor.RED + "Utilize: /warp <warp>:[jogador]";
    private static final String SINTAXE_INVALIDA = ChatColor.RED + "Utilize: /warp <warp>";
    private static final String SEM_PERMISSAO_WARP = ChatColor.RED + "Você não pode acessar a warp %s!";
    private static final String SEM_PERMISSAO_WARP_VIP = ChatColor.RED + "Para acessar esta área você precisa do grupo Campeão ou superior!";
    private static final String SEM_PERMISSAO_WARP_OUTRO = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " não pode acessar essa área!";
    private static final String SEM_PERMISSAO_WARP_VIP_OUTRO = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " não tem o grupo necessário para acessar essa área!";

    public warp(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            listarWarps(sender);
            return true;
        }

        // Juntar todos os argumentos em uma única string
        String input = String.join(" ", args);
        
        // Verificar se o comando tem a sintaxe de teleporte para outro jogador (warp:jogador)
        if (input.contains(":")) {
            return processarTeleporteOutro(sender, input);
        }

        // Caso contrário, processar como teleporte próprio
        return processarTeleporteProprio(sender, input);
    }

    private boolean processarTeleporteProprio(CommandSender sender, String warpIdentifier) {
        String warpName = obterNomeInternoWarp(warpIdentifier);
        
        if (warpName == null) {
            enviarMensagemErro(sender, String.format(WARP_NAO_ENCONTRADA, warpIdentifier));
            return true;
        }

        // Tratamento especial para warp VIP
        String warpPermission = warpName.equalsIgnoreCase("vip") ? "core.champion" : config.getString("warps." + warpName + ".permission");
        String displayName = obterDisplayNameWarp(warpName);
        
        if (warpPermission != null && !warpPermission.isEmpty()) {
            if (!(sender instanceof Player) || !((Player) sender).hasPermission(warpPermission)) {
                // Mensagem especial para warp VIP
                if (warpName.equalsIgnoreCase("vip")) {
                    enviarMensagemErro(sender, SEM_PERMISSAO_WARP_VIP);
                } else {
                    enviarMensagemErro(sender, String.format(SEM_PERMISSAO_WARP, displayName));
                }
                return true;
            }
        }

        Location warpLocation = obterLocalizacaoWarp(warpName);
        if (warpLocation == null || warpLocation.getWorld() == null) {
            enviarMensagemErro(sender, MUNDO_WARP_INVALIDO);
            return true;
        }

        if (!(sender instanceof Player)) {
            enviarMensagemErro(sender, COMANDO_CONSOLE);
            return true;
        }

        Player jogador = (Player) sender;
        UUID uuid = jogador.getUniqueId();

        if (jogador.hasPermission("core.vip") || jogador.hasPermission("core.moderator")) {
            executarTeleporte(jogador, warpLocation, String.format(SUCESSO_TELEPORTE, displayName));
            return true;
        }

        if (teleportingPlayers.contains(uuid)) {
            enviarMensagemErro(jogador, EM_ANDAMENTO);
            return true;
        }

        long ultimoTeleporte = teleportCooldowns.getOrDefault(uuid, 0L);
        long agora = System.currentTimeMillis();
        long tempoRestante = ((teleportDelay * 1000L) - (agora - ultimoTeleporte)) / 1000 + 1;

        if (agora - ultimoTeleporte < teleportDelay * 1000L) {
            enviarMensagemErro(jogador, String.format(TEMPO_ESPERA, tempoRestante));
            return true;
        }

        iniciarContagemRegressiva(jogador, warpLocation, warpName, displayName);
        return true;
    }

    private boolean processarTeleporteOutro(CommandSender sender, String input) {
        if (!sender.hasPermission("core.moderator")) {
            enviarMensagemErro(sender, PERMISSAO_OUTROS);
            return true;
        }

        // Dividir a entrada no primeiro ":" encontrado
        String[] partes = input.split(":", 2);
        
        // Verificar se tem dois pontos mas falta o nome do jogador
        if (partes.length < 2 || partes[1].trim().isEmpty() || partes[1].contains(" ")) {
            enviarMensagemErro(sender, SINTAXE_TELEPORTE_OUTRO);
            return true;
        }

        String warpIdentifier = partes[0].trim();
        String nomeAlvo = partes[1].trim();
        
        // Verificar se o jogador alvo é o próprio remetente
        if (sender instanceof Player) {
            Player jogador = (Player) sender;
            if (jogador.getName().equalsIgnoreCase(nomeAlvo)) {
                enviarMensagemErro(sender, AUTO_TELEPORTE);
                return true;
            }
        }

        String warpName = obterNomeInternoWarp(warpIdentifier);
        if (warpName == null) {
            enviarMensagemErro(sender, String.format(WARP_NAO_ENCONTRADA, warpIdentifier));
            return true;
        }

        // Tratamento especial para warp VIP
        String warpPermission = warpName.equalsIgnoreCase("vip") ? "core.champion" : config.getString("warps." + warpName + ".permission");
        String displayName = obterDisplayNameWarp(warpName);
        
        // Verificar se o remetente tem permissão para a warp (se aplicável)
        if (warpPermission != null && !warpPermission.isEmpty() && !sender.hasPermission(warpPermission)) {
            // Mensagem especial para warp VIP
            if (warpName.equalsIgnoreCase("vip")) {
                enviarMensagemErro(sender, SEM_PERMISSAO_WARP_VIP);
            } else {
                enviarMensagemErro(sender, String.format(SEM_PERMISSAO_WARP, displayName));
            }
            return true;
        }

        Location warpLocation = obterLocalizacaoWarp(warpName);
        if (warpLocation == null || warpLocation.getWorld() == null) {
            enviarMensagemErro(sender, MUNDO_WARP_INVALIDO);
            return true;
        }

        // Verificação melhorada de existência do jogador
        Player alvo = Bukkit.getPlayerExact(nomeAlvo);
        if (alvo != null) {
            // Obter nome formatado do jogador online
            String nomeFormatadoAlvo = playerNameUtils.getFormattedName(alvo);

            // Verificar se o alvo tem permissão para a warp (se aplicável)
            if (warpPermission != null && !warpPermission.isEmpty() && !alvo.hasPermission(warpPermission)) {
                // Mensagem especial para warp VIP
                if (warpName.equalsIgnoreCase("vip")) {
                    enviarMensagemErro(sender, String.format(SEM_PERMISSAO_WARP_VIP_OUTRO, nomeFormatadoAlvo));
                } else {
                    enviarMensagemErro(sender, String.format(SEM_PERMISSAO_WARP_OUTRO, nomeFormatadoAlvo));
                }
                return true;
            }

            String nomeFormatadoRemetente = getNomeFormatadoRemetente(sender);

            executarTeleporte(alvo, warpLocation, String.format(NOTIFICACAO_TELEPORTE, displayName, nomeFormatadoRemetente));
            enviarMensagemSucesso(sender, String.format(SUCESSO_TELEPORTE_OUTRO, nomeFormatadoAlvo, displayName));

            soundUtils.reproduzirSucesso(alvo);
            if (sender instanceof Player) {
                soundUtils.reproduzirSucesso((Player) sender);
            }
            return true;
        } else {
            // Jogador existe mas está offline ou nunca entrou
            playerNameUtils.getFormattedOfflineName(nomeAlvo).thenAccept(nomeFormatado -> {
                if (nomeFormatado == null) {
                    enviarMensagemErro(sender, String.format(JOGADOR_INEXISTENTE, nomeAlvo));
                } else {
                    enviarMensagemErro(sender, String.format(JOGADOR_OFFLINE, nomeFormatado));
                }
            });
            return true;
        }
    }

    private String obterNomeInternoWarp(String warpIdentifier) {
        // Primeiro verifica se a entrada é um nome interno
        if (config.contains("warps." + warpIdentifier.toLowerCase())) {
            return warpIdentifier.toLowerCase();
        }

        // Se não, procura pelo display name
        ConfigurationSection warpsSection = config.getConfigurationSection("warps");
        if (warpsSection == null) return null;

        for (String key : warpsSection.getKeys(false)) {
            String displayName = config.getString("warps." + key + ".display");
            if (displayName != null && displayName.equalsIgnoreCase(warpIdentifier)) {
                return key;
            }
        }

        return null;
    }

    private String obterDisplayNameWarp(String warpName) {
        String display = config.getString("warps." + warpName + ".display");
        return display != null ? display : warpName;
    }

    private void listarWarps(CommandSender sender) {
        ConfigurationSection warpsSection = config.getConfigurationSection("warps");
        if (warpsSection == null || warpsSection.getKeys(false).isEmpty()) {
            enviarMensagemErro(sender, ChatColor.RED + "Não existe nenhuma warp.");
            return;
        }

        // Reproduzir som de sucesso se houver warps
        if (sender instanceof Player) {
            soundUtils.reproduzirSucesso((Player) sender);
        }

        // Construir mensagem de cabeçalho
        TextComponent cabecalho = new TextComponent(LISTA_WARPS);
        cabecalho.setColor(ChatColor.GOLD);

        // Lista de warps interativas
        List<BaseComponent> componentes = new ArrayList<>();
        componentes.add(cabecalho);
        componentes.add(new TextComponent(" ")); // Espaço

        int contador = 0;
        int totalWarps = warpsSection.getKeys(false).size();
        
        // Verificar se o sender é jogador para verificar permissões
        boolean isPlayer = sender instanceof Player;
        Player playerSender = isPlayer ? (Player) sender : null;

        for (String warpKey : warpsSection.getKeys(false)) {
            contador++;
            String displayName = obterDisplayNameWarp(warpKey);
            
            // Tratamento especial para warp VIP
            String warpPermission = warpKey.equalsIgnoreCase("vip") ? "core.champion" : config.getString("warps." + warpKey + ".permission");
            boolean hasPermission = warpPermission == null || warpPermission.isEmpty() || 
                                   (isPlayer && playerSender.hasPermission(warpPermission));
            
            // Criar container para o nome e cadeado
            TextComponent container = new TextComponent();
            
            // Parte do nome da warp
            TextComponent nomeComponent = new TextComponent(displayName);
            nomeComponent.setColor(hasPermission ? ChatColor.GREEN : ChatColor.RED);
            
            container.addExtra(nomeComponent);
            
            // Adicionar cadeado se não tem permissão
            if (warpPermission != null && !warpPermission.isEmpty() && !hasPermission) {
                TextComponent cadeado = new TextComponent(" \uD83D\uDD12"); // Emoji de cadeado
                cadeado.setColor(ChatColor.RED);
                container.addExtra(cadeado);
            }
            
            // Configurar hover para todo o container
            String hoverMessage;
            if (warpPermission != null && !warpPermission.isEmpty() && !hasPermission) {
                if (warpKey.equalsIgnoreCase("vip")) {
                    hoverMessage = "Para acessar esta área você precisa\ndo grupo Campeão ou superior!";
                } else {
                    hoverMessage = "Você não pode se teleportar para a warp " + displayName + "!";
                }
            } else {
                hoverMessage = "Clique para se teleportar até a warp " + displayName + "!";
            }
            
            container.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverMessage).color(net.md_5.bungee.api.ChatColor.YELLOW).create()
            ));
            
            // Adicionar comando de clique sempre
            container.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, 
                "/warp " + warpKey
            ));
            
            componentes.add(container);
            
            // Adicionar vírgula entre os itens
            if (contador < totalWarps) {
                componentes.add(new TextComponent(ChatColor.YELLOW + ", "));
            }
        }

        // Enviar mensagem formatada
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.spigot().sendMessage(componentes.toArray(new BaseComponent[0]));
        } else {
            // Mensagem simplificada para console
            StringBuilder builder = new StringBuilder();
            for (BaseComponent comp : componentes) {
                builder.append(comp.toPlainText());
            }
            sender.sendMessage(builder.toString());
        }
    }

    private void iniciarContagemRegressiva(Player jogador, Location warp, String warpName, String displayName) {
        UUID uuid = jogador.getUniqueId();
        teleportingPlayers.add(uuid);
        teleportStartLocations.put(uuid, jogador.getLocation().clone());
        warpNames.put(uuid, warpName);

        new BukkitRunnable() {
            int segundosRestantes = teleportDelay;

            @Override
            public void run() {
                if (!jogador.isOnline() || !teleportingPlayers.contains(uuid)) {
                    cancelarTeleporte(uuid);
                    cancel();
                    return;
                }
                
                if (segundosRestantes <= 0) {
                    teleportingPlayers.remove(uuid);
                    teleportCooldowns.put(uuid, System.currentTimeMillis());
                    executarTeleporte(jogador, warp, String.format(SUCESSO_TELEPORTE, displayName));
                    teleportStartLocations.remove(uuid);
                    warpNames.remove(uuid);
                    cancel();
                    return;
                }
                
                TextComponent mensagem = new TextComponent(
                    String.format(MENSAGEM_ESPERA, displayName, segundosRestantes)
                );
                jogador.spigot().sendMessage(ChatMessageType.ACTION_BAR, mensagem);

                soundUtils.reproduzirTick(jogador);
                segundosRestantes--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoJogadorMover(PlayerMoveEvent evento) {
        Player jogador = evento.getPlayer();
        UUID uuid = jogador.getUniqueId();
        
        if (!teleportingPlayers.contains(uuid)) return;
        
        Location inicio = teleportStartLocations.get(uuid);
        if (inicio == null) return;
        
        Location atual = evento.getTo();
        
        boolean moveuHorizontalmente = Math.abs(inicio.getX() - atual.getX()) > 0.1 || 
                                       Math.abs(inicio.getZ() - atual.getZ()) > 0.1;
        
        boolean moveuVerticalmente = Math.abs(inicio.getY() - atual.getY()) > 0.1;
        
        boolean mudouBloco = inicio.getBlockX() != atual.getBlockX() || 
                            inicio.getBlockY() != atual.getBlockY() || 
                            inicio.getBlockZ() != atual.getBlockZ();
        
        if (moveuHorizontalmente || moveuVerticalmente || mudouBloco) {
            cancelarTeleporte(uuid);
            jogador.spigot().sendMessage(ChatMessageType.ACTION_BAR, MENSAGEM_CANCELAMENTO);
            soundUtils.reproduzirErro(jogador);
        }
    }

    private void cancelarTeleporte(UUID uuid) {
        teleportingPlayers.remove(uuid);
        teleportStartLocations.remove(uuid);
        warpNames.remove(uuid);
    }

    private Location obterLocalizacaoWarp(String warpName) {
        String path = "warps." + warpName;
        String nomeMundo = config.getString(path + ".world");
        if (nomeMundo == null) return null;
        
        World mundo = Bukkit.getWorld(nomeMundo);
        if (mundo == null) return null;
        
        return new Location(
            mundo,
            config.getDouble(path + ".x"),
            config.getDouble(path + ".y"),
            config.getDouble(path + ".z"),
            (float) config.getDouble(path + ".yaw"),
            (float) config.getDouble(path + ".pitch")
        );
    }

    private String getNomeFormatadoRemetente(CommandSender sender) {
        return sender instanceof Player ? 
            playerNameUtils.getFormattedName((Player) sender) : 
            "Console";
    }

    private void enviarMensagemErro(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
        if (sender instanceof Player) {
            soundUtils.reproduzirErro((Player) sender);
        }
    }

    private void enviarMensagemSucesso(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
        if (sender instanceof Player) {
            soundUtils.reproduzirSucesso((Player) sender);
        }
    }

    private void executarTeleporte(Player jogador, Location localizacao, String mensagem) {
        jogador.teleport(localizacao);
        enviarMensagemSucesso(jogador, mensagem);
        soundUtils.reproduzirSucesso(jogador);
    }
}