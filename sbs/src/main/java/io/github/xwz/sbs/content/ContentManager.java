package io.github.xwz.sbs.content;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import io.github.xwz.base.content.ContentCacheManager;
import io.github.xwz.base.content.ContentManagerBase;
import io.github.xwz.base.content.IContentManager;
import io.github.xwz.base.models.IEpisodeModel;
import io.github.xwz.sbs.api.SBSApi;
import io.github.xwz.sbs.api.SBSAuthApi;
import io.github.xwz.sbs.api.SBSRelatedApi;

public class ContentManager extends ContentManagerBase {

    private SBSApi fetchShows;

    private long lastFetchList = 0;

    public ContentManager(Context context) {
        super(context);
    }

    @Override
    public void fetchShowList(boolean force) {
        long now = (new Date()).getTime();
        boolean shouldFetch = force || now - lastFetchList > 1800000;
        Log.d("ContentManager", "diff:" + (now - lastFetchList));
        if (shouldFetch && (fetchShows == null || fetchShows.getStatus() == AsyncTask.Status.FINISHED)) {
            cache().broadcastChange(CONTENT_SHOW_LIST_FETCHING);
            fetchShows = new SBSApi(getContext());
            fetchShows.execute();
            lastFetchList = now;
        }
    }

    @Override
    public void fetchEpisode(IEpisodeModel episode) {
        broadcastChange(CONTENT_EPISODE_FETCHING, episode.getHref());
        IEpisodeModel existing = cache().getEpisode(episode.getHref());
        if (existing != null && existing.hasExtras() && existing.hasOtherEpisodes()) {
            cache().broadcastChangeDelayed(100, CONTENT_EPISODE_DONE, episode.getHref(), null);
        } else {
            new SBSRelatedApi(getContext(), episode.getHref()).execute(episode.getHref());
        }
    }

    @Override
    public void fetchAuthToken(IEpisodeModel episode) {
        cache().broadcastChange(CONTENT_AUTH_FETCHING, episode.getHref());
        new SBSAuthApi(getContext()  , episode.getHref()).execute(episode.getHref());
    }

    @Override
    public List<IEpisodeModel> getRecommendations() {
        return new ArrayList<>();
    }

    @Override
    public Class getRecommendationServiceClass() {
        return null;
    }
}
