package cn.villagebell;

import android.content.Context;
import android.graphics.*;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import java.io.*;
import java.util.zip.*;

/** Original game art, exact exported building level only. No network or nearest-level substitution. */
final class GameIcons {
    private static ZipFile archive;
    private static boolean failed;
    private static final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(8*1024*1024){
        protected int sizeOf(String key,Bitmap bitmap){return bitmap.getByteCount();}
    };
    private static synchronized ZipFile archive(Context c)throws IOException {
        if(archive==null){
            File file=new File(c.getCacheDir(),"game-icons-v3.zip");
            if(!file.exists()){
                File temp=new File(c.getCacheDir(),"game-icons-v3.tmp");
                try(InputStream in=c.getResources().openRawResource(R.raw.game_icons);OutputStream out=new FileOutputStream(temp)){
                    byte[] bytes=new byte[16384];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
                }
                if(!temp.renameTo(file))throw new IOException("Icon cache unavailable");
            }
            archive=new ZipFile(file);
        }
        return archive;
    }
    static Bitmap bitmap(Context c,Village.Upgrade u){
        if(failed)return null;
        String key=u.dataId+"_"+(u.category.startsWith("buildings")?u.level:0)+".webp";
        Bitmap hit=cache.get(key);if(hit!=null)return hit;
        try{ZipFile zip=archive(c);ZipEntry entry=zip.getEntry(key);if(entry==null)return null;
            try(InputStream in=zip.getInputStream(entry)){Bitmap b=BitmapFactory.decodeStream(in);if(b!=null)cache.put(key,b);return b;}
        }catch(IOException e){failed=true;return null;}
    }
    static View view(Context c,Village.Upgrade u,int fallback){
        Bitmap b=bitmap(c,u);if(b==null)return new GameArt(c,fallback);
        ImageView icon=new ImageView(c);icon.setImageBitmap(b);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad=Math.round(3*c.getResources().getDisplayMetrics().density);icon.setPadding(pad,pad,pad,pad);
        icon.setContentDescription(u.name+(u.category.startsWith("buildings")?"，导出等级 "+u.level+" 的游戏外观":"，游戏图标"));return icon;
    }
}
