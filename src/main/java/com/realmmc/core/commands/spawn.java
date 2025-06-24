package com.realmmc.core.commands;

import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class spawn implements CommandExecutor, Listener {

    private final FileConfiguration config;
    private final Plugin plugin;
    private final Set<UUID> teleportingPlayers = new HashSet<>();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private final Map<UUID, Location> teleportStartLocations = new HashMap<>();
    private final int teleportDelay = 5;

    private static final TextComponent MENSAGEM_CANCELAMENTO =  new TextComponent(ChatColor.RED + "O seu teleporte foi cancelado pois você se moveu!");
    private static final String MENSAGEM_ESPERA =  ChatColor.YELLOW + "Você será teleportado em %d segundos, não se mova!";
    private static final String SPAWN_NAO_DEFINIDO = ChatColor.RED + "O spawn não foi encontrado!";
    private static final String MUNDO_SPAWN_INVALIDO = ChatColor.RED + "Mundo de spawn não foi encontrado!";
    private static final String PERMISSAO_MODERADOR = ChatColor.RED + "Apenas Moderador ou superiores podem executar esse comando!";
    private static final String JOGADOR_OFFLINE = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " está offline no momento!";
    private static final String JOGADOR_INEXISTENTE = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " nunca entrou no servidor!";
    private static final String AUTO_TELEPORTE = ChatColor.RED + "Você não pode teleportar a si mesmo para o spawn!";
    private static final String SUCESSO_TELEPORTE = ChatColor.GREEN + "Teleportado para o spawn!";
    private static final String SUCESSO_TELEPORTE_OUTRO = ChatColor.GREEN + "Você teleportou o jogador " + "%s" + ChatColor.GREEN + " para o spawn!";
    private static final String NOTIFICACAO_TELEPORTE = ChatColor.YELLOW + "Você foi teleportado para o spawn por " + "%s" + ChatColor.YELLOW + "!";
    private static final String COMANDO_CONSOLE = ChatColor.RED + "Apenas jogadores podem usar este comando!";
    private static final String EM_ANDAMENTO = ChatColor.RED + "Você já tem um teleporte em andamento!";
    private static final String TEMPO_ESPERA = ChatColor.RED + "Aguarde %d segundos para se teleportar novamente!";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /spawn [jogador]";

    public spawn(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!verificarSpawnConfigurado(sender)) {
            return true;
        }

        Location spawnLocation = obterLocalizacaoSpawn();
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            enviarMensagemErro(sender, MUNDO_SPAWN_INVALIDO);
            return true;
        }

        if (args.length == 0) {
            return processarTeleporteProprio(sender, spawnLocation);
        }

        if (args.length == 1) {
            return processarTeleporteOutro(sender, args[0], spawnLocation);
        }

        enviarMensagemErro(sender, USO_CORRETO);
        return true;
    }

    private boolean verificarSpawnConfigurado(CommandSender sender) {
        if (!config.contains("spawn.world")) {
            enviarMensagemErro(sender, SPAWN_NAO_DEFINIDO);
            return false;
        }
        return true;
    }

    private boolean processarTeleporteProprio(CommandSender sender, Location spawn) {
        if (!(sender instanceof Player)) {
            enviarMensagemErro(sender, COMANDO_CONSOLE);
            return true;
        }

        Player jogador = (Player) sender;
        UUID uuid = jogador.getUniqueId();

        if (jogador.hasPermission("core.vip") || jogador.hasPermission("core.moderator")) {
            executarTeleporte(jogador, spawn, SUCESSO_TELEPORTE);
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

        iniciarContagemRegressiva(jogador, spawn);
        return true;
    }

    private boolean processarTeleporteOutro(CommandSender sender, String nomeAlvo, Location spawn) {
        if (!sender.hasPermission("core.moderator")) {
            enviarMensagemErro(sender, PERMISSAO_MODERADOR);
            return true;
        }

        if (sender instanceof Player && ((Player) sender).getName().equalsIgnoreCase(nomeAlvo)) {
            enviarMensagemErro(sender, AUTO_TELEPORTE);
            return true;
        }

        // Correção 1: playerExists()
        if (!playerNameUtils.playerExists(nomeAlvo)) {
            enviarMensagemErro(sender, String.format(JOGADOR_INEXISTENTE, nomeAlvo));
            return true;
        }

        Player alvo = Bukkit.getPlayer(nomeAlvo);
        if (alvo == null) {
            tratarJogadorOffline(sender, nomeAlvo);
            return true;
        }

        // Correção 2: getFormattedName()
        String nomeFormatadoAlvo = playerNameUtils.getFormattedName(alvo);
        String nomeFormatadoRemetente = getNomeFormatadoRemetente(sender);

        executarTeleporte(alvo, spawn, String.format(NOTIFICACAO_TELEPORTE, nomeFormatadoRemetente));
        enviarMensagemSucesso(sender, String.format(SUCESSO_TELEPORTE_OUTRO, nomeFormatadoAlvo));

        soundUtils.reproduzirSucesso(alvo);
        if (sender instanceof Player) {
            soundUtils.reproduzirSucesso((Player) sender);
        }

        return true;
    }

    private void tratarJogadorOffline(CommandSender sender, String nomeJogador) {
        // Correção 3: getCorrectName() e getFormattedOfflineName()
        String nomeCorreto = playerNameUtils.getCorrectName(nomeJogador);
        CompletableFuture<String> nomeFormatado = playerNameUtils.getFormattedOfflineName(nomeCorreto);
        nomeFormatado.thenAccept(formattedName -> {
            // Correção 4: Executar na thread principal
            Bukkit.getScheduler().runTask(plugin, () -> 
                enviarMensagemErro(sender, String.format(JOGADOR_OFFLINE, formattedName))
            );
        });
    }

    private String getNomeFormatadoRemetente(CommandSender sender) {
        // Correção 5: getFormattedName()
        return sender instanceof Player ?
                playerNameUtils.getFormattedName((Player) sender) :
                "Console";
    }

    private void iniciarContagemRegressiva(Player jogador, Location spawn) {
        UUID uuid = jogador.getUniqueId();
        teleportingPlayers.add(uuid);
        teleportStartLocations.put(uuid, jogador.getLocation().clone());

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
                    executarTeleporte(jogador, spawn, SUCESSO_TELEPORTE);
                    teleportStartLocations.remove(uuid);
                    cancel();
                    return;
                }

                TextComponent mensagem = new TextComponent(
                        String.format(MENSAGEM_ESPERA, segundosRestantes)
                );
                jogador.spigot().sendMessage(ChatMessageType.ACTION_BAR, mensagem);

                soundUtils.reproduzirTick(jogador);
                segundosRestantes--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void executarTeleporte(Player jogador, Location localizacao, String mensagem) {
        jogador.teleport(localizacao);
        enviarMensagemSucesso(jogador, mensagem);
        soundUtils.reproduzirSucesso(jogador);
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
    }

    private Location obterLocalizacaoSpawn() {
        String nomeMundo = config.getString("spawn.world");
        if (nomeMundo == null) return null;

        World mundo = Bukkit.getWorld(nomeMundo);
        if (mundo == null) return null;

        return new Location(
                mundo,
                config.getDouble("spawn.x"),
                config.getDouble("spawn.y"),
                config.getDouble("spawn.z"),
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch")
        );
    }

    private void enviarMensagemErro(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
        if (sender instanceof Player) {
            soundUtils.reproduzirErro((Player) sender);
        }
    }

    private void enviarMensagemSucesso(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
    }
}