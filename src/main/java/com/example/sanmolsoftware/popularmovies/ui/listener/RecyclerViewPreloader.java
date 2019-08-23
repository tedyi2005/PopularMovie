

package com.example.sanmolsoftware.popularmovies.ui.listener;

import android.support.v7.widget.RecyclerView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.util.Util;

import java.util.List;
import java.util.Queue;


public abstract class RecyclerViewPreloader<T> extends RecyclerView.OnScrollListener {

    private final int maxPreload;
    private final PreloadTargetQueue preloadTargetQueue;
    private final PreloadModelProvider<T> preloadModelProvider;
    private final PreloadSizeProvider<T> preloadDimensionProvider;

    private int lastEnd;
    private int lastStart;
    private int lastFirstVisible;
    private int totalItemCount;

    private boolean isIncreasing = true;

    public interface PreloadModelProvider<U> {
        List<U> getPreloadItems(int position);
        GenericRequestBuilder getPreloadRequestBuilder(U item);
    }


    public interface PreloadSizeProvider<T> {
        int[] getPreloadSize(T item, int adapterPosition, int perItemPosition);
    }

    @Deprecated
    public RecyclerViewPreloader(int maxPreload) {
        this.preloadModelProvider = new PreloadModelProvider<T>() {
            @Override
            public List<T> getPreloadItems(int position) {
                return getItems(position, position + 1);
            }

            @Override
            public GenericRequestBuilder getPreloadRequestBuilder(T item) {
                return getRequestBuilder(item);
            }
        };
        this.preloadDimensionProvider = new PreloadSizeProvider<T>() {

            @Override
            public int[] getPreloadSize(T item, int adapterPosition, int perItemPosition) {
                return getDimensions(item);
            }
        };
        this.maxPreload = maxPreload;
        preloadTargetQueue = new PreloadTargetQueue(maxPreload + 1);

    }

    public RecyclerViewPreloader(PreloadModelProvider<T> preloadModelProvider,
                                 PreloadSizeProvider<T> preloadDimensionProvider, int maxPreload) {
        this.preloadModelProvider = preloadModelProvider;
        this.preloadDimensionProvider = preloadDimensionProvider;
        this.maxPreload = maxPreload;
        preloadTargetQueue = new PreloadTargetQueue(maxPreload + 1);
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        // Do nothing.
    }


    public abstract void onScrolled(RecyclerView recyclerView, int dx, int dy);

    public void processScroll(int firstVisible, int visibleCount, int totalCount) {
        totalItemCount = totalCount;
        if (firstVisible > lastFirstVisible) {
            preload(firstVisible + visibleCount, true);
        } else if (firstVisible < lastFirstVisible) {
            preload(firstVisible, false);
        }
        lastFirstVisible = firstVisible;
    }

    @Deprecated
    protected int[] getDimensions(T item) {
        throw new IllegalStateException("You must either provide a PreloadDimensionProvider or override "
                + "getDimensions()");
    }


    @Deprecated
    protected List<T> getItems(int start, int end) {
        throw new IllegalStateException("You must either provide a PreloadModelProvider or override getItems()");
    }


    @SuppressWarnings("rawtypes")
    @Deprecated
    protected GenericRequestBuilder getRequestBuilder(T item) {
        throw new IllegalStateException("You must either provide a PreloadModelProvider, or override "
                + "getRequestBuilder()");
    }

    private void preload(int start, boolean increasing) {
        if (isIncreasing != increasing) {
            isIncreasing = increasing;
            cancelAll();
        }
        preload(start, start + (increasing ? maxPreload : -maxPreload));
    }

    private void preload(int from, int to) {
        int start;
        int end;
        if (from < to) {
            start = Math.max(lastEnd, from);
            end = to;
        } else {
            start = to;
            end = Math.min(lastStart, from);
        }
        end = Math.min(totalItemCount, end);
        start = Math.min(totalItemCount, Math.max(0, start));

        if (from < to) {
            // Increasing
            for (int i = start; i < end; i++) {
                preloadAdapterPosition(this.preloadModelProvider.getPreloadItems(i), i, true);
            }
        } else {
            // Decreasing
            for (int i = end - 1; i >= start; i--) {
                preloadAdapterPosition(this.preloadModelProvider.getPreloadItems(i), i, false);
            }
        }

        lastStart = start;
        lastEnd = end;
    }

    private void preloadAdapterPosition(List<T> items, int position, boolean isIncreasing) {
        final int numItems = items.size();
        if (isIncreasing) {
            for (int i = 0; i < numItems; ++i) {
                preloadItem(items.get(i), position, i);
            }
        } else {
            for (int i = numItems - 1; i >= 0; --i) {
                preloadItem(items.get(i), position, i);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void preloadItem(T item, int position, int i) {
        final int[] dimensions = this.preloadDimensionProvider.getPreloadSize(item, position, i);
        if (dimensions != null) {
            GenericRequestBuilder preloadRequestBuilder = this.preloadModelProvider.getPreloadRequestBuilder(item);
            preloadRequestBuilder.into(preloadTargetQueue.next(dimensions[0], dimensions[1]));
        }
    }

    private void cancelAll() {
        for (int i = 0; i < maxPreload; i++) {
            Glide.clear(preloadTargetQueue.next(0, 0));
        }
    }

    private static final class PreloadTargetQueue {
        private final Queue<PreloadTarget> queue;

        public PreloadTargetQueue(int size) {
            queue = Util.createQueue(size);

            for (int i = 0; i < size; i++) {
                queue.offer(new PreloadTarget());
            }
        }

        public PreloadTarget next(int width, int height) {
            final PreloadTarget result = queue.poll();
            queue.offer(result);
            result.photoWidth = width;
            result.photoHeight = height;
            return result;
        }
    }

    private static class PreloadTarget extends BaseTarget<Object> {
        private int photoHeight;
        private int photoWidth;

        @Override
        public void onResourceReady(Object resource,
                                    GlideAnimation<? super Object> glideAnimation) {
            // Do nothing.
        }

        @Override
        public void getSize(SizeReadyCallback cb) {
            cb.onSizeReady(photoWidth, photoHeight);
        }
    }
}

