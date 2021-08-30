package mindustry.plugin;

import arc.files.Fi;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.plugin.discordcommands.Command;
import mindustry.plugin.discordcommands.Context;
import mindustry.plugin.discordcommands.DiscordCommands;
import mindustry.plugin.discordcommands.RoleRestrictedCommand;
import mindustry.world.modules.ItemModule;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static mindustry.Vars.state;
import static mindustry.Vars.world;
import static mindustry.plugin.Utils.*;
import static mindustry.plugin.ioMain.getTextChannel;

public class ComCommands {
    public void registerCommands(DiscordCommands handler) {
        handler.registerCommand(new Command("chat") {
            {
                help = "<message> Sends a message to in-game chat.";
            }

            public void run(Context ctx) {
                if (ctx.event.isPrivateMessage()) return;

                EmbedBuilder eb = new EmbedBuilder();
                ctx.message = escapeCharacters(ctx.message);
                if (ctx.message.length() < chatMessageMaxSize) {
                    Call.sendMessage("[sky]" + ctx.author.getName() + " @discord >[] " + ctx.message);
                    eb.setTitle("Command executed");
                    eb.setDescription("Your message was sent successfully..");
                    ctx.channel.sendMessage(eb);
                } else {
                    ctx.reply("Message too big.");
                }
            }
        });
        handler.registerCommand(new Command("downloadmap") {
            {
                help = "<mapname/mapid> Preview and download a server map in a .msav file format.";
            }

            public void run(Context ctx) {
                if (ctx.args.length < 2) {
                    ctx.reply("Not enough arguments, use `%map <mapname/mapid>`".replace("%", ioMain.prefix));
                    return;
                }

                Map found = getMapBySelector(ctx.message.trim());
                if (found == null) {
                    ctx.reply("Map not found!");
                    return;
                }

                Fi mapFile = found.file;

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(escapeCharacters(found.name()))
                        .setDescription(escapeCharacters(found.description()))
                        .setAuthor(escapeCharacters(found.author()));
                // TODO: .setImage(mapPreviewImage)
                ctx.channel.sendMessage(embed, mapFile.file());
            }
        });
        handler.registerCommand(new Command("players") {
            {
                help = "Check who is online and their ids.";
            }

            public void run(Context ctx) {
                StringBuilder msg = new StringBuilder("**Players online: " + Groups.player.size() + "**\n```\n");
                for (Player player : Groups.player) {
                    msg.append("· ").append(escapeCharacters(player.name)).append(" : ").append(player.id).append("\n");
                }
                msg.append("```");
                ctx.channel.sendMessage(msg.toString());
            }
        });
        handler.registerCommand(new Command("info") {
            {
                help = "Get basic server information.";
            }

            public void run(Context ctx) {
                try {
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTitle(ioMain.serverName)
                            .addField("Players", String.valueOf(Groups.player.size()))
                            .addField("Map", Vars.state.map.name())
                            .addField("Wave", String.valueOf(state.wave))
                            .addField("Next wave in", Math.round(state.wavetime / 60) + " seconds.");

                    ctx.channel.sendMessage(eb);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    ctx.reply("An error has occurred.");
                }
            }
        });
        handler.registerCommand(new Command("resinfo") {
            {
                help = "Check the amount of resources in the core.";
            }

            public void run(Context ctx) {
                if (!state.rules.waves) {
                    ctx.reply("Only available in survival mode!");
                    return;
                }
                // the normal player team is "sharded"
                TeamData data = state.teams.get(Team.sharded);
                //-- Items are shared between cores
//                CoreEntity core = data.cores.first();
                ItemModule core = Groups.player.first().core().items;
//                ItemModule items = core.items;
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("Resources in the core:");
//                items.forEach((item, amount) -> eb.addInlineField(item.name, String.valueOf(amount)));

                eb.addInlineField("copper: ", core.get(Items.copper) + "\n");
                eb.addInlineField("lead: ", core.get(Items.lead) + "\n");
                eb.addInlineField("graphite: ", core.get(Items.graphite) + "\n");
                eb.addInlineField("metaglass: ", core.get(Items.metaglass) + "\n");
                eb.addInlineField("titanium: ", core.get(Items.titanium) + "\n");
                eb.addInlineField("thorium: ", core.get(Items.thorium) + "\n");
                eb.addInlineField("silicon: ", core.get(Items.silicon) + "\n");
                eb.addInlineField("plastanium: ", core.get(Items.plastanium) + "\n");
                eb.addInlineField("phase fabric: ", core.get(Items.phaseFabric) + "\n");
                eb.addInlineField("surge alloy: ", core.get(Items.surgeAlloy) + "\n");

                ctx.channel.sendMessage(eb);
            }
        });

        handler.registerCommand(new Command("help") {
            {
                help = "Display all available commands and their usage.";
            }

            public void run(Context ctx) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Public commands:");
                EmbedBuilder embed2 = new EmbedBuilder()
                        .setTitle("Restricted commands:");
                for (Command command : handler.getAllCommands()) {
                    if (command instanceof RoleRestrictedCommand) {
                        embed2.addField("**" + command.name + "**", command.help);
                    } else {
                        embed.addField("**" + command.name + "**", command.help);
                    }
                }
                ctx.channel.sendMessage(embed2);
                ctx.channel.sendMessage(embed);
            }
        });

//        handler.registerCommand(new Command("redeem") {
//            {
//                help = "<name|id> Promote your in-game rank. [NOTE: Abusing this power and giving it to other players will result in a ban.]";
//            }
//
//            public void run(Context ctx) {
//                CompletableFuture.runAsync(() -> {
//                    EmbedBuilder eb = new EmbedBuilder();
//                    String target = "";
//                    if (ctx.args.length > 1) {
//                        target = ctx.args[1];
//                    }
//                    List<Role> authorRoles = ctx.author.asUser().get().getRoles(ctx.event.getServer().get()); // javacord gay
//                    List<String> roles = new ArrayList<>();
//                    for (Role r : authorRoles) {
//                        if (r != null) {
//                            roles.add(r.getIdAsString());
//                        }
//                    }
//                    if (target.length() > 0) {
//                        int rank = 0;
//                        for (String role : roles) {
//                            if (rankRoles.containsKey(role)) {
//                                if (rankRoles.get(role) > rank) {
//                                    rank = rankRoles.get(role);
//                                }
//                            }
//                        }
//                        Player player = findPlayer(target);
//                        if (player != null && rank > 0) {
//                            PlayerData pd = getData(player.uuid);
//                            if (pd != null) {
//                                pd.rank = rank;
//                                setData(player.uuid, pd);
//                            }
//                            eb.setTitle("Command executed successfully");
//                            eb.setDescription("Promoted " + escapeCharacters(player.name) + " to " + escapeColorCodes(rankNames.get(rank).name) + ".");
//                            ctx.channel.sendMessage(eb);
//                            player.con.kick("Your rank was modified, please rejoin.", 0);
//                        } else {
//                            eb.setTitle("Command terminated");
//                            eb.setDescription("Player not online or not found.");
//                            ctx.channel.sendMessage(eb);
//                        }
//
//                    } else {
//                        eb.setTitle("Command terminated");
//                        eb.setDescription("Invalid arguments provided or no roles to redeem.");
//                        ctx.channel.sendMessage(eb);
//                    }
//                });
//            }
//
//        });

        TextChannel warningsChannel = null;
        if (ioMain.data.has("warnings_chat_channel_id")) {
            warningsChannel = getTextChannel(ioMain.data.getString("warnings_chat_channel_id"));
        }
    }
}