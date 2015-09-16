package io.github.xwz.iview.content;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.xwz.base.ImmutableMap;
import io.github.xwz.base.content.ContentManagerBase;
import io.github.xwz.base.models.IEpisodeModel;
import io.github.xwz.iview.api.AuthApi;
import io.github.xwz.iview.api.EpisodeDetailsApi;
import io.github.xwz.iview.api.TvShowListApi;

public class ContentManager extends ContentManagerBase {

    public static final Map<String, String> CATEGORIES = ImmutableMap.of(
            "arts", "Arts & Culture",
            "comedy", "Comedy",
            "docs", "Documentary",
            "drama", "Drama",
            "education", "Education",
            "lifestyle", "Lifestyle",
            "news", "News & Current Affairs",
            "panel", "Panel & Discussion",
            "sport", "Sport"
    );

    private static final Map<String, String> CHANNELS = ImmutableMap.of(
            "abc1", "ABC1",
            "abc2", "ABC2",
            "abc3", "ABC3",
            "abc4kids", "ABC4Kids",
            "iview", "iView Exclusives"
    );

    public ContentManager(Context context) {
        super(context);
    }

    private TvShowListApi fetchShows;
    private long lastFetchList = 0;

    @Override
    public void fetchShowList(boolean force) {
        long now = (new Date()).getTime();
        boolean shouldFetch = force || now - lastFetchList > 1800000;
        if (shouldFetch && (fetchShows == null || fetchShows.getStatus() == AsyncTask.Status.FINISHED)) {
            broadcastChange(CONTENT_SHOW_LIST_FETCHING);
            fetchShows = new TvShowListApi(getContext());
            fetchShows.execute();
            lastFetchList = now;
        }
    }

    @Override
    public void fetchAuthToken(IEpisodeModel episode) {
        cache().broadcastChange(CONTENT_AUTH_FETCHING, episode.getHref());
        new AuthApi(getContext(), episode.getHref()).execute(episode.getStream());
    }

    @Override
    public void fetchEpisode(IEpisodeModel episode) {
        broadcastChange(CONTENT_EPISODE_FETCHING, episode.getHref());
        IEpisodeModel existing = getEpisode(episode.getHref());
        if (existing != null && existing.hasExtras() && existing.hasOtherEpisodes()) {
            cache().broadcastChangeDelayed(100, CONTENT_EPISODE_DONE, episode.getHref(), null);
        } else {
            new EpisodeDetailsApi(getContext(), episode.getHref()).execute(episode.getHref());
        }
    }

    @Override
    public LinkedHashMap<String, List<IEpisodeModel>> getAllShowsByCategories() {
        List<IEpisodeModel> shows = getAllShows();
        LinkedHashMap<String, List<IEpisodeModel>> all = new LinkedHashMap<>();
        all.putAll(cache().getCollections());

        for (Map.Entry<String, String> channel : CHANNELS.entrySet()) {
            List<IEpisodeModel> episodes = new ArrayList<>();
            for (IEpisodeModel show : shows) {
                if (channel.getKey().equals(show.getChannel())) {
                    episodes.add(show);
                }
            }
            all.put(channel.getValue(), episodes);
        }
        for (Map.Entry<String, String> cat : CATEGORIES.entrySet()) {
            List<IEpisodeModel> episodes = new ArrayList<>();
            for (IEpisodeModel show : shows) {
                if (show.getCategories().contains(cat.getKey())) {
                    episodes.add(show);
                }
            }
            all.put(cat.getValue(), episodes);
        }
        return all;
    }

    @Override
    public List<IEpisodeModel> getRecommendations() {
        List<IEpisodeModel> all = getAllShows();
        if (all.size() > 40) {
            return getAllShows().subList(30, 32);
        }
        return new ArrayList<>();
    }

    @Override
    public Class getRecommendationServiceClass() {
        return RecommendationsService.class;
    }
}
