package cn.villagebell;
import android.content.Context;
import android.graphics.*;
import android.view.View;

final class ProgressTrack extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Village.Upgrade upgrade;
    private final int color;
    ProgressTrack(Context c,Village.Upgrade u,int color){super(c);this.upgrade=u;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){float h=getHeight();p.setColor(0xffe5dfd0);c.drawRoundRect(0,0,getWidth(),h,h/2,h/2,p);float fraction=Dashboard.waitingProgress(upgrade,System.currentTimeMillis());p.setColor(color);if(fraction>0)c.drawRoundRect(0,0,Math.max(h,getWidth()*fraction),h,h/2,h/2,p);}
}
