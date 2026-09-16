package cn.villagebell;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original vector artwork. Decorative village, never a reconstruction of the player's layout. */
final class GameArt extends View {
    static final int VILLAGE=0, BUILDING=1, RESEARCH=2, PET=3, NIGHT=4, CROWN=5, BELL=6, BOOK=7, CANNON=8, TROOP=9;
    private final int kind;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    GameArt(Context c,int kind){super(c);this.kind=kind;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();
        if(kind==VILLAGE){canvas.scale(getWidth()/360f,getHeight()/192f);scene(canvas);}
        else {float size=Math.min(getWidth(),getHeight());canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/100f,size/100f);icon(canvas,kind);}
        canvas.restore();
    }
    private void fill(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
    private void rect(Canvas c,float x,float y,float w,float h,int color){fill(color);c.drawRect(x,y,x+w,y+h,p);}
    private void round(Canvas c,float x,float y,float w,float h,float r,int color){fill(color);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
    private void oval(Canvas c,float x,float y,float w,float h,int color){fill(color);c.drawOval(x,y,x+w,y+h,p);}
    private void poly(Canvas c,int color,float...xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();fill(color);c.drawPath(path,p);}
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float width){fill(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,x2,y2,p);}
    private void tree(Canvas c,float x,float y,float s){c.save();c.translate(x,y);c.scale(s,s);oval(c,-12,15,26,8,0x16342d20);rect(c,-2,0,4,19,0xff906c46);poly(c,0xff54784a,-15,9,0,-24,15,9);poly(c,0xff719452,-11,-3,0,-29,11,-3);c.restore();}
    private void scene(Canvas c){
        p.setShader(new LinearGradient(0,0,0,192,0xffcfe2c1,0xffedf0d6,Shader.TileMode.CLAMP));c.drawRect(0,0,360,192,p);p.setShader(null);
        oval(c,273,14,30,30,0xffffe9a6);oval(c,22,17,67,14,0xffeef3df);oval(c,58,10,42,19,0xffeef3df);
        poly(c,0xffb4ccab,0,79,51,31,89,68,152,20,206,64,258,37,360,81,360,140,0,140);
        poly(c,0xff90b18a,0,113,69,76,113,94,177,55,234,99,306,64,360,103,360,169,0,169);
        oval(c,24,130,320,48,0x273a542f);
        poly(c,0xff718748,18,113,179,50,342,111,180,183);
        poly(c,0xffa9bb69,18,107,179,45,342,105,180,176);
        poly(c,0xffbbcb7d,34,107,179,54,328,105,180,166);
        line(c,90,91,266,137,0xffdccc91,15);line(c,93,139,267,83,0xffdccc91,13);
        for(int i=0;i<8;i++){oval(c,40+i*36,114+(i%2)*21,3,2,0xffe5dcaa);}
        tree(c,27,91,.85f);tree(c,327,88,.9f);tree(c,50,136,.75f);tree(c,305,139,.7f);tree(c,200,53,.58f);
        c.save();c.translate(105,47);c.scale(.83f,.83f);icon(c,BUILDING);c.restore();
        c.save();c.translate(226,72);c.scale(.55f,.55f);icon(c,RESEARCH);c.restore();
        c.save();c.translate(72,105);c.scale(.47f,.47f);icon(c,PET);c.restore();
        c.save();c.translate(190,118);c.scale(.45f,.45f);icon(c,NIGHT);c.restore();
        c.save();c.translate(41,77);c.scale(.38f,.38f);icon(c,CANNON);c.restore();
        // Construction props and stone boundary.
        for(int i=0;i<4;i++){round(c,119+i*14,151+i*3,11,8,2,0xffd0c2a0);rect(c,120+i*14,151+i*3,9,3,0xffe7dcba);}
        line(c,210,102,210,126,0xff795736,3);poly(c,0xffdc9946,211,102,230,105,211,113);
        oval(c,158,142,8,4,0xfff5e3aa);oval(c,264,119,7,3,0xffeee0a7);
    }
    private void icon(Canvas c,int k){
        if(k==BUILDING){
            oval(c,8,81,84,14,0x253b3520);
            poly(c,0xff8b826d,18,52,50,39,83,52,83,83,50,94,18,82);
            poly(c,0xffe0cda4,18,48,50,36,50,88,18,77);
            poly(c,0xffb7a27b,50,36,83,48,83,78,50,88);
            poly(c,0xffb74f36,10,50,48,19,90,49,51,66);
            poly(c,0xffe68d4b,10,50,48,19,50,53,34,59);
            poly(c,0xffd06c38,50,53,48,19,90,49,70,59);
            round(c,39,64,16,24,6,0xff5f5142);round(c,43,67,8,19,3,0xff866541);
            rect(c,23,60,9,12,0xff795f48);rect(c,65,60,9,11,0xff795f48);rect(c,25,61,5,7,0xffffd679);rect(c,67,61,5,6,0xffffd679);
            line(c,49,8,49,27,0xff6a5536,3);poly(c,0xfff4cd66,50,8,69,11,50,19);
            line(c,18,79,34,84,0xfff3e3bf,2);
        }else if(k==RESEARCH){
            oval(c,13,84,76,10,0x253b3520);round(c,22,33,55,54,11,0xffa292c0);round(c,26,35,47,49,9,0xffd6cbea);
            poly(c,0xff776594,30,36,30,18,66,18,66,36,76,72,22,72);poly(c,0xffad8fd1,34,36,34,24,62,24,62,38,70,69,27,69);
            poly(c,0xffbd66b7,29,58,67,58,73,77,67,84,28,84,22,77);oval(c,28,54,40,9,0xffe6a3d8);
            round(c,27,13,42,11,3,0xffd7b870);rect(c,34,15,28,3,0xfff5dea1);
            oval(c,40,67,10,10,0xffefb8e6);oval(c,55,73,6,6,0xffe4a2d9);line(c,33,34,27,47,0xfff1eaf8,3);
            oval(c,75,28,7,7,0xffd3b7e3);oval(c,81,16,4,4,0xffb58ccc);
        }else if(k==PET){
            oval(c,9,83,83,10,0x253b3520);round(c,19,38,64,49,9,0xffc39b63);poly(c,0xff729779,10,46,49,13,91,46);poly(c,0xff96b68c,10,46,49,13,50,40);
            round(c,34,51,34,36,17,0xff6b6350);oval(c,42,65,18,16,0xffe0b976);oval(c,35,59,8,10,0xffe0b976);oval(c,45,53,8,10,0xffe0b976);oval(c,56,55,8,10,0xffe0b976);oval(c,63,63,7,9,0xffe0b976);
            rect(c,23,44,53,5,0xffa67c4d);line(c,24,83,30,83,0xffebd0a0,3);
        }else if(k==NIGHT){
            oval(c,9,84,83,9,0x253b3520);poly(c,0xff8891ae,22,46,51,35,78,46,78,83,50,92,22,82);poly(c,0xffb4bfcd,22,46,51,35,51,89,22,82);
            poly(c,0xff435d85,11,48,47,16,89,45,53,61);poly(c,0xff6e85a5,11,48,47,16,53,48,36,57);
            round(c,42,66,17,24,4,0xff54617a);rect(c,26,60,9,12,0xffefd080);rect(c,64,57,8,12,0xffefd080);
            oval(c,69,6,21,21,0xfff5cf79);oval(c,76,2,19,19,0xffdce5e7);line(c,83,36,88,36,0xfff4d27e,2);line(c,85.5f,33,85.5f,39,0xfff4d27e,2);
        }else if(k==CROWN){
            oval(c,11,81,80,12,0x253b3520);poly(c,0xffb98433,16,36,32,49,48,23,64,49,84,32,76,80,24,80);poly(c,0xffefc360,18,34,32,48,48,23,64,48,82,32,72,71,26,71);
            round(c,24,72,52,12,3,0xffbd8535);rect(c,29,74,42,4,0xffefc96f);poly(c,0xffb66068,48,49,57,59,48,68,40,59);
            oval(c,13,28,9,9,0xfff7d77a);oval(c,44,17,9,9,0xfff7d77a);oval(c,78,26,9,9,0xfff7d77a);
        }else if(k==BELL){
            oval(c,12,83,76,10,0x253b3520);oval(c,43,11,14,18,0xffbd8535);oval(c,40,76,20,16,0xffb47b31);
            round(c,24,24,52,54,25,0xffe6b958);poly(c,0xffe6b958,25,48,74,48,82,77,18,77);round(c,16,73,68,10,4,0xffbb8534);line(c,34,38,30,62,0xfffbe5a3,5);
        }else if(k==CANNON){
            oval(c,8,82,83,12,0x253b3520);poly(c,0xff8b816b,14,66,46,53,85,68,85,82,48,95,14,81);poly(c,0xffcfc5a8,14,66,46,53,85,68,48,82);
            round(c,31,45,36,30,10,0xffa17843);oval(c,24,52,22,27,0xff694e34);oval(c,29,58,12,15,0xffc49b60);
            c.save();c.rotate(-27,50,48);round(c,27,30,53,28,5,0xff65736e);rect(c,32,31,45,8,0xff8b9991);round(c,68,28,17,32,5,0xff45514e);oval(c,72,32,11,24,0xff263d36);line(c,37,45,60,45,0xffa8b4a7,3);c.restore();
        }else if(k==TROOP){
            oval(c,13,85,74,8,0x253b3520);poly(c,0xff997849,19,26,50,15,81,26,76,66,50,91,25,66);poly(c,0xff719267,25,30,50,21,75,30,70,63,50,83,30,63);
            poly(c,0xffe6e4cb,47,24,54,15,60,25,53,61,45,59);poly(c,0xffaabbb1,54,15,60,25,53,61,50,58);
            line(c,38,60,59,64,0xffe5bc5f,7);line(c,48,64,45,77,0xff755139,6);oval(c,40,76,9,8,0xffe5bc5f);
        }else{
            round(c,20,17,61,72,6,0xff997144);round(c,16,12,61,72,6,0xffe5cc95);rect(c,21,12,7,72,0xffad7c46);
            line(c,36,31,65,31,0xffad7c46,4);line(c,36,43,65,43,0xffad7c46,4);line(c,36,55,56,55,0xffad7c46,4);
        }
    }
}
