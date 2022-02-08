package com.bumptech.glide.integration.webp.decoder;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.bumptech.glide.integration.webp.WebpHeaderParser;
import com.bumptech.glide.integration.webp.WebpImage;
import com.bumptech.glide.load.resource.gif.GifOptions;

import static com.bumptech.glide.integration.webp.decoder.WebpDecoder.DEFAULT_MAX_FRAME_BITMAP_CACHE_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} that decodes {@link
 *  com.bumptech.glide.integration.webp.decoder.WebpDrawable} from {@link java.nio.ByteBuffer} data
 *
 * @author liuchun
 */
public class ByteBufferWebpDecoder implements ResourceDecoder<ByteBuffer, WebpDrawable> {
    public static final Option<Boolean> DISABLE_ANIMATION = Option.memory(
            "com.bumptech.glide.integration.webp.decoder.ByteBufferWebpDecoder.DisableAnimation", false);

    private final Context mContext;
    private final BitmapPool mBitmapPool;
    private final GifBitmapProvider mProvider;

    public ByteBufferWebpDecoder(Context context) {
        this(context, Glide.get(context).getArrayPool(),
                Glide.get(context).getBitmapPool());
    }

    public ByteBufferWebpDecoder(Context context, ArrayPool byteArrayPool, BitmapPool bitmapPool) {
        this.mContext = context.getApplicationContext();
        this.mBitmapPool = bitmapPool;
        this.mProvider = new GifBitmapProvider(bitmapPool, byteArrayPool);
    }

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
        if (options.get(DISABLE_ANIMATION)) {
            return false;
        }

        WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(source);
        return WebpHeaderParser.isAnimatedWebpType(webpType);
    }

    @Nullable
    @Override
    public Resource<WebpDrawable> decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {

        int length = source.remaining();
        byte[] data = new byte[length];
        source.get(data, 0, length);

        WebpImage webp = WebpImage.create(data);

        Bitmap.Config config =
                options.get(GifOptions.DECODE_FORMAT) == DecodeFormat.PREFER_RGB_565
                        ? Bitmap.Config.RGB_565
                        : Bitmap.Config.ARGB_8888;

        int sampleSize = Utils.getSampleSize(webp.getWidth(), webp.getHeight(), width, height);
        WebpFrameCacheStrategy cacheStrategy = options.get(WebpFrameLoader.FRAME_CACHE_STRATEGY);
        WebpDecoder webpDecoder = new WebpDecoder(mProvider, webp, source, sampleSize, cacheStrategy, DEFAULT_MAX_FRAME_BITMAP_CACHE_SIZE);
        webpDecoder.setDefaultBitmapConfig(config);
        webpDecoder.advance();
        Bitmap firstFrame = webpDecoder.getNextFrame();
        if (firstFrame == null) {
            return null;
        }

        Transformation<Bitmap> unitTransformation = UnitTransformation.get();

        return new WebpDrawableResource(new WebpDrawable(mContext, webpDecoder, mBitmapPool, unitTransformation, width, height,
                firstFrame));
    }
}
