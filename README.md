#AndroidTopFaceSDKSample使用说明
<br>
[官网地址](http://www.voome.cn)
<br>
##接入流程
###依赖库导入
<br>
眼镜试戴模块，所依赖的库文件为: topface-release.aar，需添加到Android项目中。
<br>
###授权认证
<br>
调用com.topplusvision.topface.TopFace的init(getApplicationContext(), " client_id", " client_secret");
说明：申请 client_id 和 client_secret 后调用此函数获得授权。不调用认证函数无法使用人脸检测功能，正确调用认证函数即可正常使用。
<br>
[获取License](http://www.voome.cn/register/index.shtml)
<br>
###SDK初始化
<br>
在检测之前调用com.topplusvision.topface.TopFace的initWithFocus(Context context, float focus)初始化检测上下文
<br>
##接口定义和使用说明
```Java
/**
* 初始化TopFace
* @param context 上下文
* @param focus 焦距
* @return 返回码，-1表示初始化失败，0表示初始化成功
*/
public static int initWithFocus(Context context,float focus) ;


/**
 * 检测人脸
 * @param nv21 摄像头数据 yuv格式
 * @param width  摄像头数据宽度
 * @param height 摄像头数据高度
 * @param bytePerPix 每个像素点的 byte
 * @param rotation 摄像头数据旋转角度
 * @return长度为151的数组，第0~135位表示68个人脸特征点二维像素坐标，原点是传入图像的左上角，特征点代表意义参考示意图；第136~138位表示人脸鼻尖处在相机坐标系下的位置数据，坐标系定义：x轴向右,y轴向下,z轴向前；第139~141位表示人脸相对相机的姿态数据，单位是弧度，依次定义为：pitc俯仰角、roll翻滚角、yaw偏航角；第142位表示置信度.
 */

public static float[] dynamicDetect(byte[] nv21, int width, int height, float bytePerPix, int rotation);

```
<br>
##68个人脸特征点二维像素坐标图
![](https://github.com/topplus/AndroidTopFaceSDKSample/raw/master/images/feature.jpg)