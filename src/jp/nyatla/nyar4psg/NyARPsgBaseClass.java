/* 
 * PROJECT: NyARToolkit for proce55ing.
 * --------------------------------------------------------------------------------
 * The MIT License
 * Copyright (c) 2008 nyatla
 * airmail(at)ebony.plala.or.jp
 * http://nyatla.jp/nyartoolkit/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 */

package jp.nyatla.nyar4psg;


import javax.media.opengl.*;

import processing.core.*;
import processing.opengl.*;


import jp.nyatla.nyartoolkit.*;
import jp.nyatla.nyartoolkit.core.param.*;
import jp.nyatla.nyartoolkit.core.rasterreader.NyARPerspectiveRasterReader;
import jp.nyatla.nyartoolkit.core.transmat.*;
import jp.nyatla.nyartoolkit.core.types.NyARDoublePoint2d;
import jp.nyatla.nyartoolkit.core.types.NyARDoublePoint3d;
import jp.nyatla.nyartoolkit.core.types.matrix.NyARDoubleMatrix44;



/**
 * このクラスは、NyARToolkit for Processingのベースクラスです。
 * ARToolkit座標系の環境定数、環境設定機能を継承クラスに対して提供します。
 */
class NyARPsgBaseClass
{


	/**
	 * バージョン文字列です。
	 * NyAR4psgのバージョン情報を示します。
	 */
	public final static String VERSION = "NyAR4psg/1.0.1;NyARToolkit for java/3.0.0+;ARToolKit/2.72.1";
	/**　参照するAppletのインスタンスです。*/
	protected PApplet _ref_papplet;	
	/**　ProcessingスタイルのProjectionMatrixです。*/
	protected final PMatrix3D _ps_projection=new PMatrix3D();
	/**　ARToolkitパラメータのインスタンスです。*/
	protected final NyARParam _ar_param=new NyARParam();
	protected final NyARFrustum _frustum=new NyARFrustum();;
	protected NyAR4PsgConfig _config;
	
	/** 入力画像ラスタです。{@link PImage}をラップします。継承クラスで入力画像をセットします。*/
	protected PImageRaster _src_raster;
	/** 画像抽出用のオブジェクトです。{@link #_src_raster}を参照します。*/
	protected NyARPerspectiveRasterReader _preader;
	private int _g_type;
	/**
	 * コンストラクタです。
	 */
	protected NyARPsgBaseClass()
	{
	}
	protected void initInstance(PApplet parent,String i_cparam_file, int i_width,int i_height,NyAR4PsgConfig i_config) throws NyARException
	{
		this._ref_papplet=parent;
		this._config=i_config;
		this._src_raster=new PImageRaster(i_width,i_height);
		this._preader=new NyARPerspectiveRasterReader(this._src_raster.getBufferType());
		try{
			this._g_type=getGraphicsType(parent.g);
			this._ar_param.loadARParam(this._ref_papplet.createInput(i_cparam_file));
			this._ar_param.changeScreenSize(i_width, i_height);

			//ProcessingのprojectionMatrixの計算と、Frustumの計算
			arPerspectiveMat2Projection(this._ar_param.getPerspectiveProjectionMatrix(),i_width,i_height,this._ps_projection,this._frustum);
		}catch(NyARException e){
			this._ref_papplet.die("Error while setting up NyARToolkit for java", e);
		}
		return;		
	}
	private final static double view_distance_min = 100;
	private final static double view_distance_max = 100000;
	private final static int GT_P3D=0;
	private final static int GT_OPENGL=1;
	/**
	 * {@link PGraphics}をグラフィクス定数に変換します。
	 * @param i_g
	 * @return
	 * @throws NyARException
	 */
	private static int getGraphicsType(PGraphics i_g) throws NyARException 
	{
		String n=i_g.getClass().getName();
		if(n.compareTo("processing.opengl.PGraphicsOpenGL")==0){
			return GT_OPENGL;
		}else if(n.compareTo("processing.core.PGraphics3D")==0){
			return GT_P3D;
		}
		throw new NyARException("Unknown Graphics");
		
	}


	private float[] _tmpf=new float[16];

	/**
	 * この関数は、ARToolKit準拠のProjectionMatrixをProcessingにセットします。
	 * 関数を実行すると、ProcessingのProjectionMatrixがARToolKitのカメラパラメータのものに変わり、映像にマッチした描画ができるようになります。
	 * ProcessingのデフォルトFrustumに戻すときは、{@link PGraphics3D#perspective()}を使います。
	 * Frustumの有効期間は、次に{@link PGraphics3D#perspective()}か{@link PGraphics3D#perspective()}をコールするまでです。
	 * @return
	 * 置き換えら得る前のprojectionMatrixを返します。
	 */
	public PMatrix3D setARPerspective()
	{
		return this.setPerspective(this._ps_projection);
	}
	/**
	 * この関数は、ProjectionMatrixをProcessingにセットします。
	 * @param i_projection
	 * 設定するProjectionMatrixを指定します。
	 * @return
	 * 置き換えら得る前のprojectionMatrixを返します。
	 */	
	public PMatrix3D setPerspective(PMatrix3D i_projection)
	{
		if(!(this._ref_papplet.g instanceof PGraphics3D)){
			this._ref_papplet.die("NyAR4Psg require PGraphics3D instance.");
		}
		PGraphics3D g=(PGraphics3D)this._ref_papplet.g;
		//現在のProjectionMatrixを保存する。
		PMatrix3D ret=new PMatrix3D();
		ret.set(g.projection);
		//ProjectionMatrixの設定
		g.projection.set(i_projection);
		//OpenGLの時はちょっと細工
		if(this._g_type==GT_OPENGL)
		{
			GL gl=((PGraphicsOpenGL)g).gl;
			gl.glMatrixMode(GL.GL_PROJECTION);
			PMatrix2GLProjection(i_projection,_tmpf);
			gl.glLoadMatrixf(_tmpf,0);
		}
		return ret;
	}

	protected static void PMatrix2GLProjection(PMatrix3D i_in,float[] o_out)
	{
		o_out[ 0]=i_in.m00;
		o_out[ 1]=i_in.m10;
		o_out[ 2]=i_in.m20;
		o_out[ 3]=i_in.m30;
		o_out[ 4]=i_in.m01;
		o_out[ 5]=i_in.m11;
		o_out[ 6]=i_in.m21;
		o_out[ 7]=i_in.m31;
		o_out[ 8]=i_in.m02;
		o_out[ 9]=i_in.m12;
		o_out[10]=i_in.m22;
		o_out[11]=i_in.m32;
		o_out[12]=i_in.m03;
		o_out[13]=i_in.m13;
		o_out[14]=i_in.m23;
		o_out[15]=i_in.m33;		
	}
	protected static void PMatrix2GLProjection(PMatrix3D i_in,double[] o_out)
	{
		o_out[ 0]=i_in.m00;
		o_out[ 1]=i_in.m10;
		o_out[ 2]=i_in.m20;
		o_out[ 3]=i_in.m30;
		o_out[ 4]=i_in.m01;
		o_out[ 5]=i_in.m11;
		o_out[ 6]=i_in.m21;
		o_out[ 7]=i_in.m31;
		o_out[ 8]=i_in.m02;
		o_out[ 9]=i_in.m12;
		o_out[10]=i_in.m22;
		o_out[11]=i_in.m32;
		o_out[12]=i_in.m03;
		o_out[13]=i_in.m13;
		o_out[14]=i_in.m23;
		o_out[15]=i_in.m33;	
	}
	
	private static void arPerspectiveMat2Projection(NyARPerspectiveProjectionMatrix i_prjmat,int i_w,int i_h,PMatrix3D o_projection,NyARFrustum o_frustum)
	{
		NyARDoubleMatrix44 tmp=new NyARDoubleMatrix44();
		i_prjmat.makeCameraFrustumRH(i_w, i_h, view_distance_min, view_distance_max,tmp);
		o_projection.m00=(float)(tmp.m00);
		o_projection.m01=(float)(tmp.m01);
		o_projection.m02=(float)(tmp.m02);
		o_projection.m03=(float)(tmp.m03);
		o_projection.m10=(float)(tmp.m10);
		o_projection.m11=(float)(tmp.m11);
		o_projection.m12=(float)(tmp.m12);
		o_projection.m13=(float)(tmp.m13);
		o_projection.m20=(float)(tmp.m20);
		o_projection.m21=(float)(tmp.m21);
		o_projection.m22=(float)(tmp.m22);
		o_projection.m23=(float)(tmp.m23);
		o_projection.m30=(float)(tmp.m30);
		o_projection.m31=(float)(tmp.m31);
		o_projection.m32=(float)(tmp.m32);
		o_projection.m33=(float)(tmp.m33);
		o_frustum.setValue(tmp, i_w, i_h);
	}
	protected static void matResult2GLArray(NyARTransMatResult i_src,double[] o_gl_array)
	{
		o_gl_array[0 + 0 * 4] = i_src.m00; 
		o_gl_array[0 + 1 * 4] = i_src.m01;
		o_gl_array[0 + 2 * 4] = i_src.m02;
		o_gl_array[0 + 3 * 4] = i_src.m03;
		o_gl_array[1 + 0 * 4] = -i_src.m10;
		o_gl_array[1 + 1 * 4] = -i_src.m11;
		o_gl_array[1 + 2 * 4] = -i_src.m12;
		o_gl_array[1 + 3 * 4] = -i_src.m13;
		o_gl_array[2 + 0 * 4] = -i_src.m20;
		o_gl_array[2 + 1 * 4] = -i_src.m21;
		o_gl_array[2 + 2 * 4] = -i_src.m22;
		o_gl_array[2 + 3 * 4] = -i_src.m23;
		o_gl_array[3 + 0 * 4] = 0.0;
		o_gl_array[3 + 1 * 4] = 0.0;
		o_gl_array[3 + 2 * 4] = 0.0;
		o_gl_array[3 + 3 * 4] = 1.0;
	}
	/**
	 * 左手系変換用の行列
	 */
	private final static PMatrix3D _lh_mat=new PMatrix3D(-1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1);
	
	/**
	 * 変換行列をProcessingのMatrixへ変換します。
	 * @param i_src
	 * @param i_mode
	 * @param o_pmatrix
	 */
	protected static void matResult2PMatrix3D(NyARDoubleMatrix44 i_src,int i_mode,PMatrix3D o_pmatrix)
	{
		o_pmatrix.m00 = (float)i_src.m00; 
		o_pmatrix.m01 = (float)i_src.m01;
		o_pmatrix.m02 = (float)i_src.m02;
		o_pmatrix.m03 = (float)i_src.m03;
		o_pmatrix.m10 = (float)i_src.m10;//mirror
		o_pmatrix.m11 = (float)i_src.m11;//mirror
		o_pmatrix.m12 = (float)i_src.m12;//mirror
		o_pmatrix.m13 = (float)i_src.m13;//mirror
		o_pmatrix.m20 = (float)-i_src.m20;
		o_pmatrix.m21 = (float)-i_src.m21;
		o_pmatrix.m22 = (float)-i_src.m22;
		o_pmatrix.m23 = (float)-i_src.m23;
		o_pmatrix.m30 = 0.0f;
		o_pmatrix.m31 = 0.0f;
		o_pmatrix.m32 = 0.0f;
		o_pmatrix.m33 = 1.0f;
		if(i_mode==NyAR4PsgConfig.CS_LEFT_HAND)
		{
			o_pmatrix.apply(_lh_mat);
		}
	}
	/**
	 * この関数は、i_mat平面から、自由変形した画像を取得します。
	 * @param i_mat
	 * @param i_x
	 * @param i_y
	 * @return
	 */
	protected PVector screen2MarkerCoordSystem(NyARDoubleMatrix44 i_mat,int i_x,int i_y)
	{
		PVector ret=new PVector();
		NyARDoublePoint3d tmp=new NyARDoublePoint3d();
		this._frustum.unProjectOnMatrix(i_x, i_y,i_mat,tmp);
		ret.x=(float)tmp.x;
		ret.y=(float)tmp.y;
		ret.z=(float)tmp.z;
		if(this._config._coordinate_system==NyAR4PsgConfig.CS_LEFT_HAND){
			ret.x*=-1;
		}
		return ret;
	}
	/**
	 * PImageをラップしたラスタから画像を得ます。
	 * @param i_mat
	 * @param i_x1
	 * @param i_y1
	 * @param i_x2
	 * @param i_y2
	 * @param i_x3
	 * @param i_y3
	 * @param i_x4
	 * @param i_y4
	 * @param i_out_w_pix
	 * @param i_out_h_pix
	 * @return
	 */
	protected PImage pickupMarkerImage(NyARDoubleMatrix44 i_mat,int i_x1,int i_y1,int i_x2,int i_y2,int i_x3,int i_y3,int i_x4,int i_y4,int i_out_w_pix,int i_out_h_pix)
	{
		//WrapRasterの内容チェック
		if(!this._src_raster.hasBuffer()){
			this._ref_papplet.die("_rel_detector is null.(Function detect() was never called. )");
		}
		PImage img=new PImage(i_out_w_pix,i_out_h_pix);
		img.parent=this._ref_papplet;
		try{
			NyARDoublePoint3d[] pos=NyARDoublePoint3d.createArray(4);
			i_mat.transform3d(i_x1, i_y1,0,	pos[1]);
			i_mat.transform3d(i_x2, i_y2,0,	pos[0]);
			i_mat.transform3d(i_x3, i_y3,0,	pos[3]);
			i_mat.transform3d(i_x4, i_y4,0,	pos[2]);
			//4頂点を作る。
			NyARDoublePoint2d[] pos2=NyARDoublePoint2d.createArray(4);
			for(int i=3;i>=0;i--){
				this._frustum.project(pos[i],pos2[i]);
			}
			PImageRaster out_raster=new PImageRaster(i_out_w_pix,i_out_h_pix);
			out_raster.wrapBuffer(img);
			if(!this._preader.read4Point(this._src_raster,pos2,0,0,1,out_raster))
			{
				throw new Exception("this._preader.read4Point failed.");
			}
			return img;
		}catch(Exception e){
			e.printStackTrace();
			this._ref_papplet.die("Exception occurred at MultiARTookitMarker.pickupImage");
			return null;
		}
	}	
	/**
	 * この関数は、スクリーン座標を撮像点座標に変換します。
	 * 撮像点の座標系は、カメラ座標系になります。
	 * <p>公式 - 
	 * この関数は、gluUnprojectのビューポートとモデルビュー行列を固定したものです。
	 * 公式は、以下の物使用しました。
	 * http://www.opengl.org/sdk/docs/man/xhtml/gluUnProject.xml
	 * ARToolKitの座標系に合せて計算するため、OpenGLのunProjectとはix,iyの与え方が違います。画面上の座標をそのまま与えてください。
	 * </p>
	 * @param ix
	 * スクリーン上の座標
	 * @param iy
	 * 画像上の座標
	 * @param o_point_on_screen
	 * 撮像点座標
	 */
/*	public final PVector unProject(double ix,double iy)
	{
		double n=(this._frustum_rh.m23/(this._frustum_rh.m22-1));
		NyARDoubleMatrix44 m44=this._inv_frustum_rh;
		double v1=(this._screen_size.w-ix-1)*2/this._screen_size.w-1.0;//ARToolKitのFrustramに合せてる。
		double v2=(this._screen_size.h-iy-1)*2/this._screen_size.h-1.0;
		double v3=2*n-1.0;
		double b=1/(m44.m30*v1+m44.m31*v2+m44.m32*v3+m44.m33);
		o_point_on_screen.x=(m44.m00*v1+m44.m01*v2+m44.m02*v3+m44.m03)*b;
		o_point_on_screen.y=(m44.m10*v1+m44.m11*v2+m44.m12*v3+m44.m13)*b;
		o_point_on_screen.z=(m44.m20*v1+m44.m21*v2+m44.m22*v3+m44.m23)*b;
		return;
	}
*/	/**
	 * この関数は、スクリーン上の点と原点を結ぶ直線と、任意姿勢の平面の交差点を、カメラの座標系で取得します。
	 * この座標は、カメラ座標系です。
	 * @param ix
	 * スクリーン上の座標
	 * @param iy
	 * スクリーン上の座標
	 * @param i_mat
	 * 平面の姿勢行列です。
	 * @param o_pos
	 * 結果を受け取るオブジェクトです。
	 */
/*	public final PVector unProjectOnCamera(double ix,double iy,PMatrix3D i_mat)
	{
		//画面→撮像点
		this.unProject(ix,iy,o_pos);
		//撮像点→カメラ座標系
		double nx=i_mat.m02;
		double ny=i_mat.m12;
		double nz=i_mat.m22;
		double mx=i_mat.m03;
		double my=i_mat.m13;
		double mz=i_mat.m23;
		double t=(nx*mx+ny*my+nz*mz)/(nx*o_pos.x+ny*o_pos.y+nz*o_pos.z);
		o_pos.x=t*o_pos.x;
		o_pos.y=t*o_pos.y;
		o_pos.z=t*o_pos.z;
	}	
*/	/**
	 * 画面上の点と原点を結ぶ直線と任意姿勢の平面の交差点を、平面の座標系で取得します。
	 * ARToolKitの本P175周辺の実装と同じです。
	 * @param ix
	 * スクリーン上の座標
	 * @param iy
	 * スクリーン上の座標
	 * @param i_mat
	 * 平面の姿勢行列です。
	 * @param o_pos
	 * 結果を受け取るオブジェクトです。
	 * @return
	 * 計算に成功すると、trueを返します。
	 */
/*	public final PVector unProjectOnMatrix(double ix,double iy,PMatrix3D i_mat)
	{
		//交点をカメラ座標系で計算
		unProjectOnCamera(ix,iy,i_mat,o_pos);
		//座標系の変換
		NyARDoubleMatrix44 m=new NyARDoubleMatrix44();
		if(!m.inverse(i_mat)){
			return false;
		}
		m.transform3d(o_pos, o_pos);
		return true;
	}
*/	/**
	 * カメラ座標系の点を、スクリーン座標の点へ変換します。
	 * @param i_x
	 * カメラ座標系の点
	 * @param i_y
	 * カメラ座標系の点
	 * @param i_z
	 * カメラ座標系の点
	 * @param o_pos2d
	 * 結果を受け取るオブジェクトです。
	 */
/*	public final PVector project(double i_x,double i_y,double i_z)
	{
		NyARDoubleMatrix44 m=this._frustum_rh;
		double v3_1=1/i_z*m.m32;
		double w=this._screen_size.w;
		double h=this._screen_size.h;
		o_pos2d.x=w-(1+(i_x*m.m00+i_z*m.m02)*v3_1)*w/2;
		o_pos2d.y=h-(1+(i_y*m.m11+i_z*m.m12)*v3_1)*h/2;
		return;
	}
*/}

