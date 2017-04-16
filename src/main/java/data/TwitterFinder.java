package data;

import discord.Message;
import exceptions.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.StatusAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 12/01/2017.
 */
public class TwitterFinder extends StatusAdapter{
    private final static Logger LOG = LoggerFactory.getLogger(TwitterFinder.class);
    protected static Map<String, TwitterFinder> twitterChannels;
    private String channelId;

    public TwitterFinder(String channelId) {
        this.channelId = channelId;

        ClientConfig.TWITTER().addListener(this);

        FilterQuery query = new FilterQuery();
        query.follow(Constants.dofusTwitter);
        ClientConfig.TWITTER().filter(query);
    }

    @Override
    public void onStatus(Status status) {
        // In case if channel didn't exist anymore and it is not removed at time
        if (getTwitterChannels().containsKey(getChannelId())){
            if (status.getUser().getId() == Constants.dofusTwitter) {
                EmbedObject.AuthorObject author = new EmbedObject.AuthorObject("@" + status.getUser().getScreenName(),
                        "https://twitter.com/" + status.getUser().getScreenName(),
                        status.getUser().getMiniProfileImageURL(), null);

                EmbedObject embedTweet = new EmbedObject("Tweet", null, status.getText(),
                        "https://twitter.com/" + status.getUser().getScreenName() + "/status/" + status.getId(),
                        null, 1942002, null, null,
                        null, null,null, author, null);

                Message.sendEmbed(ClientConfig.DISCORD().getChannelByID(getChannelId()), embedTweet);
            }
        }
    }

    public static Map<String, TwitterFinder> getTwitterChannels(){
        if (twitterChannels == null) {
            twitterChannels = new HashMap<>();

            Connexion connexion = Connexion.getInstance();
            Connection connection = connexion.getConnection();

            try {
                PreparedStatement query = connection.prepareStatement("SELECT id_chan FROM Twitter");
                ResultSet resultSet = query.executeQuery();

                while (resultSet.next()){
                    IChannel chan = ClientConfig.DISCORD().getChannelByID(resultSet.getString("id_chan"));
                    if (chan != null)
                        twitterChannels.put(chan.getStringID(), new TwitterFinder(chan.getStringID()));
                    else
                        new TwitterFinder(resultSet.getString("id_chan")).removeToDatabase();
                }
            } catch (SQLException e) {
                Reporter.report(e);
                LOG.error(e.getMessage());
            }
        }
        return twitterChannels;
    }

    public void addToDatabase(){
        if (! getTwitterChannels().containsKey(getChannelId())) {
            getTwitterChannels().put(getChannelId(), this);
            Connexion connexion = Connexion.getInstance();
            Connection connection = connexion.getConnection();

            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO Twitter(id_chan) VALUES(?);");
                preparedStatement.setString(1, getChannelId());

                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                Reporter.report(e);
                LOG.error(e.getMessage());
            }
        }
    }

    public void removeToDatabase() {
        getTwitterChannels().remove(getChannelId());

        Connexion connexion = Connexion.getInstance();
        Connection connection = connexion.getConnection();

        try {
            PreparedStatement request = connection.prepareStatement("DELETE FROM Twitter WHERE id_chan = ?;");
            request.setString(1, getChannelId());
            request.executeUpdate();

        } catch (SQLException e) {
            Reporter.report(e);
            LOG.error(getChannelId() + " : " + e.getMessage());
        }
    }

    public String getChannelId(){
        return channelId;
    }
}
