import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.net.Socket;

import javax.media.*;
import javax.media.datasink.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.util.*;
import javax.media.control.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import com.sun.image.codec.jpeg.*;
 
// import com.sun.media.vfw.VFWCapture;         // JMF 2.1.1c version
import com.sun.media.protocol.vfw.VFWCapture;   // JMF 2.1.1e version
 
public class JWebCam extends JFrame implements WindowListener,  ComponentListener{
 
    protected final static int MIN_WIDTH  = 100;  // 320; //최소 넓이?
    protected final static int MIN_HEIGHT = 100;  // 240; // 최대 넓이?
 
    protected static int shotCounter = 1;  //?????????? 뭔가막 늘어나는데 쓰는 변수인듯?? 찾아봐야함
 
    protected JLabel statusBar = null; //시작시키면 밑에 바(초기화중입니다 어디연결되었습니다등의 이름)
    protected JPanel visualContainer = null; 
	protected JPanel chatContainer = null;
    protected Component visualComponent = null; //컴포넌트 : 독립된 모듈..?????????????????????????
    protected JToolBar toolbar = null; //위에 위치한 툴바
    protected MyToolBarAction formatButton    = null; 
    protected MyToolBarAction captureButton   = null;
 
    protected Player player = null; //플레이어를 하는 뭔가 그런건듯
    protected CaptureDeviceInfo webCamDeviceInfo = null; //디지바이스를 캡쳐해주는거
    protected MediaLocator mediaLocator = null; //미디어를 가져오는 친구인듯??????????????????????????????????
    protected Dimension imageSize = null; //패널같은 폼인듯?
    protected FormatControl formatControl = null; //???????????????????????????
 
    protected VideoFormat currentFormat = null;//??????????
    protected Format[] videoFormats = null;//???????????
    protected MyVideoFormat[] myFormatList = null; //JWebCam.MyVideoFormat
    protected MyCaptureDeviceInfo[] myCaptureDevices = null; //JWebCam.MyCaptureDeviceInfo
 
    protected boolean initialised = false; //JWebCam.initialised   웹캠감지
    
 
    /* --------------------------------------------------------------
     * Constructor  생성자 
     * -------------------------------------------------------------- */
 
    
    /*
     *   레이아웃 컴포넌트
     */
    public JWebCam (String frameTitle){ //시작
        super(frameTitle); //프레임타이틀을 받아오네
        try{
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); //UI를 바꾸는 건가봄
        }catch (Exception cnfe){
            System.out.println ("UI 에러");
        }
 
        setSize (800, 600); // default size...
 
        addWindowListener (this);
        addComponentListener (this);
 
        getContentPane().setLayout(new BorderLayout());

        
        visualContainer = new JPanel();
		chatContainer = new JPanel();
		chatContainer = new JPanel();


        visualContainer.setLayout ( new BorderLayout() );
 
        getContentPane().add ( visualContainer, BorderLayout.CENTER );
 
        statusBar = new JLabel (""){
        	// 불쾌한 버그 해결 방법
        	// 최소 JLabel 크기는 상태 표시줄의 텍스트에 의해 결정됨
        	// 그래서 레이아웃 관리자는 비디오 이미지의 윈도우를 축소하지 않습니다.
            public Dimension getPreferredSize (  ){
            	// JLABEL이 최소 10픽셀의 폭을 "허용"하도록 합니다.
            	// 텍스트 길이에서 최소 크기를 계산하지 않음
                return ( new Dimension ( 10, super.getPreferredSize().height ) );
            }
        };
 
        statusBar.setBorder ( new EtchedBorder() );
        getContentPane().add ( statusBar, BorderLayout.SOUTH );

    }
 
    /* --------------------------------------------------------------
     * Initialise   초기화
     *
     * @returns true if web cam is detected 웹 캠이 감지되면 true를 반환합니다.
     * -------------------------------------------------------------- */
 
    public boolean initialise() throws Exception{
        MyCaptureDeviceInfo[] cams = autoDetect();
        
        if ( cams.length > 0 ){
            if ( cams.length == 1 ){
                 System.out.println ("웹캠 1개 감지");
                 return ( initialise ( cams[0].capDevInfo ) );
            }else{
                System.out.println ("웹캠 " + cams.length + "개 감지 ");
                Object selected = JOptionPane.showInputDialog (this,
                                                               "비디오 포멧 설정",
                                                               "Capture format selection",
                                                               JOptionPane.INFORMATION_MESSAGE,
                                                               null, //  Icon icon,
                                                               cams, // videoFormats,
                                                               cams[0] );
                if ( selected != null ){
                    return (initialise(((MyCaptureDeviceInfo)selected).capDevInfo));
                }else{
                    return (initialise(null));
                }
            }
        }else{
            return (initialise(null));
        }
    }
 
    /* -------------------------------------------------------------------
     * Initialise 초기화
     *
     * @params _deviceInfo, specific web cam device if not autodetected
     * 						특정 웹캠 장치(자동 감지되지 않은 경우)
     * @returns true if web cam is detected
     * 웹 캠이 감지되면 true를 반환합니다.
     * ------------------------------------------------------------------- */
    
    				//기본false
    public boolean initialise (CaptureDeviceInfo _deviceInfo) throws Exception{
        statusBar.setText ("초기화중입니다."); //맨밑에 글쓰기 
        webCamDeviceInfo = _deviceInfo; // 디지바이스 캡쳐 = _deviceInfo 뭔지모르겠음?????????????????????
        
        System.out.println("webCamDeviceInfo ======= " + webCamDeviceInfo);
        if ( webCamDeviceInfo != null ){
        	
            statusBar.setText ( "연결성공 : " + webCamDeviceInfo.getName() +" 로 연결함." );
 
            try{
                setUpToolBar(); //툴바를 세팅
                getContentPane().add ( toolbar, BorderLayout.NORTH );
 
                mediaLocator = webCamDeviceInfo.getLocator();
                if (mediaLocator != null){
                    player = Manager.createRealizedPlayer(mediaLocator);
                    if (player != null){
                        player.start();
                        formatControl = (FormatControl)player.getControl("javax.media.control.FormatControl");
                        videoFormats = webCamDeviceInfo.getFormats();
 
                        myFormatList = new MyVideoFormat[videoFormats.length];
                        for (int i=0; i < videoFormats.length; i++){        //?
                            myFormatList[i] = new MyVideoFormat ((VideoFormat)videoFormats[i]);
                        }
 
                        Format currFormat = formatControl.getFormat(); ///?
 
                        visualComponent = player.getVisualComponent();
                        if ( visualComponent != null ){
                            visualContainer.add ( visualComponent, BorderLayout.CENTER );
 
                            if ( currFormat instanceof VideoFormat ){
                                 currentFormat = (VideoFormat)currFormat;
								 
                                 imageSize = currentFormat.getSize();
                                 visualContainer.setPreferredSize ( imageSize );
                                 setSize ( imageSize.width, imageSize.height + statusBar.getHeight() + toolbar.getHeight() );
                            }else{
                                System.err.println ("Error : Cannot get current video format");
                            }
 
                            invalidate();
                            pack();
                            return ( true );
                        }else{
                            System.err.println ("Error : Could not get visual component");
                            System.out.println( "에러 : 비쥬얼 컴포넌트 오류");
                            return ( false );
                        }
                    }else{
                        System.err.println ("Error : Cannot create player");
                        System.err.println ("에러 :  플레이어 생성이 안됨");
                        statusBar.setText ( "Cannot create player" );
                        return ( false );
                    }
                }else{
                    System.err.println ("Error : No MediaLocator for " + webCamDeviceInfo.getName() );
                    statusBar.setText ( "No Media Locator for : " + webCamDeviceInfo.getName() );
                    return ( false );
                }
            }catch ( IOException ioEx ){
                System.err.println ("Error connecting to [" + webCamDeviceInfo.getName() + "] : " + ioEx.getMessage() );
 
                statusBar.setText ( "Connecting to : " + webCamDeviceInfo.getName() );
 
                return ( false );
            }catch ( NoPlayerException npex ){
                statusBar.setText ("Cannot create player");
                return ( false );
            }catch ( CannotRealizeException nre ){
                statusBar.setText ( "Cannot realize player");
                return ( false );
            }
        }else{
            return ( false );
        }
    }
 
 
    /* -------------------------------------------------------------------
     * Dynamically create menu items  동적으로 메뉴 항목 생성
     *
     * @returns the device info object if found, null otherwise 
     * 발견된 경우 디바이스 정보 개체를 반환하고, 그렇지 않으면 null을 반환합니다.
     * ------------------------------------------------------------------- */
 
    public void setFormat (VideoFormat selectedFormat){
        if (formatControl != null){
            player.stop();
 
            currentFormat = selectedFormat;
 
            if (visualComponent != null){
                 visualContainer.remove (visualComponent);
            }
 
            imageSize = currentFormat.getSize(); //포멧의 크기를 가져옴
            visualContainer.setPreferredSize (imageSize); //기본크기 설정
 
            statusBar.setText ("Format : " + currentFormat);
            System.out.println ("Format : " + currentFormat);
 
            formatControl.setFormat (currentFormat);
 
            player.start();
 
            visualComponent = player.getVisualComponent();
            if (visualComponent != null){
                visualContainer.add ( visualComponent, BorderLayout.CENTER );
            }
 
            invalidate();       // let the layout manager work out the sizes
            pack();
        }else{
            System.out.println ("Visual component not an instance of FormatControl");
            statusBar.setText ("Visual component cannot change format");
        }
    }
 
    public VideoFormat getFormat ( ){
        return (currentFormat);
    }
 
 
 
    protected void setUpToolBar ( ){ //툴바세팅하기
        toolbar = new JToolBar();
 
 
        // 참고: 도구 모음을 도킹 해제 및 도킹할 때 외관상의 결함으로 인해 나는 이것을 거짓으로 설정했다.
        toolbar.setFloatable(false);
 
        // 참고: 16 x 16비트 맵을 제공하는 경우 MyToolBarAction 생성기에서 주석 처리된 라인을 교체할 수 있습니다.
         formatButton    = new MyToolBarAction ( "해상도", "BtnFormat.jpg" );
        captureButton   = new MyToolBarAction ( "캡쳐하기",    "BtnCapture.jpg" );
 
        toolbar.add ( formatButton );
        toolbar.add ( captureButton );
 
        getContentPane().add ( toolbar, BorderLayout.NORTH );
    }
 
 
    protected void toolbarHandler (MyToolBarAction actionBtn){ //툴바 핸들러
        if (actionBtn == formatButton){
            Object selected = JOptionPane.showInputDialog (this,
                                                           "비디오 해상도 설정",
                                                           "캡쳐하기",
                                                           JOptionPane.INFORMATION_MESSAGE,
                                                           null,        //  Icon icon,
                                                           myFormatList, // videoFormats,
                                                           currentFormat);
            if (selected != null){
                setFormat (((MyVideoFormat)selected).format);
            }
        }else if (actionBtn == captureButton){
            Image photo = grabFrameImage();
            if (photo != null){
                MySnapshot snapshot = new MySnapshot (photo, new Dimension (imageSize));
            }else{
                System.err.println ("Error : Could not grab frame");
            }
        }
    }
 
 
    /* -------------------------------------------------------------------
     * 시스템에서 첫 번째 웹 카메라를 자동 감지합니다.
     * 윈도우(vfw) 캡처 장치에 대한 비디오 검색
     *
     * @장치 정보 개체(발견된 경우)는 null이고, 그렇지 않은 경우 null입니다.
     * ------------------------------------------------------------------- */
 
    public MyCaptureDeviceInfo[] autoDetect(){
        Vector list = CaptureDeviceManager.getDeviceList (null);
 
        CaptureDeviceInfo devInfo = null;
        String name;
        Vector capDevices = new Vector();
        
        if (list != null){
 
            for (int i=0; i < list.size(); i++){
                devInfo = (CaptureDeviceInfo)list.elementAt ( i );
                name = devInfo.getName();
 
                if (name.startsWith ("vfw:")){
                    System.out.println ("DeviceManager List : " + name );
                    capDevices.addElement (new MyCaptureDeviceInfo (devInfo));
                }
            }
        }else{
            for (int i = 0; i < 10; i++){
                try{
                    name = VFWCapture.capGetDriverDescriptionName (i);
                    if (name != null && name.length() > 1){
                        devInfo = com.sun.media.protocol.vfw.VFWSourceStream.autoDetect (i);
                        if ( devInfo != null ){
                            System.out.println ("VFW Autodetect List : " + name );
                            capDevices.addElement (new MyCaptureDeviceInfo (devInfo));
                        }
                    }
                }catch (Exception ioEx){
                    System.err.println ("Error connecting to [" + webCamDeviceInfo.getName() + "] : " + ioEx.getMessage());
 
                    // ignore errors detecting device
                    statusBar.setText ("AutoDetect failed : " + ioEx.getMessage());
                }
            }
        }
 
        MyCaptureDeviceInfo[] detected = new MyCaptureDeviceInfo[ capDevices.size() ];
        for ( int i=0; i < capDevices.size(); i++ ){
            detected[i] = (MyCaptureDeviceInfo)capDevices.elementAt ( i );
        }
 
        return ( detected );
    }
 
 
    /* -------------------------------------------------------------------
     * deviceInfo 장치 정보
     *
     * @note outputs text information 텍스트 정보 출력
     * ------------------------------------------------------------------- */
 
    public void deviceInfo ( ){
        if ( webCamDeviceInfo != null ){
            Format[] formats = webCamDeviceInfo.getFormats();
 
            //if ( ( formats != null ) && ( formats.length > 0 ) ){}
 
            for ( int i=0; i < formats.length; i++ ){
                Format aFormat = formats[i];
                if ( aFormat instanceof VideoFormat ){
                    Dimension dim = ((VideoFormat)aFormat).getSize();
                    // System.out.println ("Video Format " + i + " : " + formats[i].getEncoding() + ", " + dim.width + " x " + dim.height );
                }
            }
        }else{
            System.out.println ("Error : No web cam detected");
        }
    }
 
    /* -------------------------------------------------------------------
     * grabs a frame's buffer from the web cam / device
     * 웹캠/장치에서 프레임 버퍼를 잡습니다.
     *
     * @returns A frames buffer
     * 프레임 버퍼를 반환합니다.
     * ------------------------------------------------------------------- */
 
    public Buffer grabFrameBuffer ( ){
        if ( player != null ){
            FrameGrabbingControl fgc = (FrameGrabbingControl)player.getControl ( "javax.media.control.FrameGrabbingControl" );
            if ( fgc != null ){
                return ( fgc.grabFrame() );
            }else{
                System.err.println ("Error : FrameGrabbingControl is null");
                return ( null );
            }
        }else{
            System.err.println ("Error : Player is null");
            return ( null );
        }
    }
 
 
    /* -------------------------------------------------------------------
     * grabs a frame's buffer, as an image, from the web cam / device
     * 웹캠/장치에서 프레임의 버퍼를 이미지로 캡처합니다.
     *
     * @returns A frames buffer as an image
     * 프레임 버퍼를 이미지로 반환합니다.
     * ------------------------------------------------------------------- */
 
    public Image grabFrameImage ( ){
        Buffer buffer = grabFrameBuffer();
        if ( buffer != null ){
            // Convert it to an image
            BufferToImage btoi = new BufferToImage ( (VideoFormat)buffer.getFormat() );
            if ( btoi != null ){
                Image image = btoi.createImage ( buffer );
                if ( image != null ){
                    return ( image );
                }else{
                    System.err.println ("Error : BufferToImage cannot convert buffer");
                    return ( null );
                }
            }else{
                System.err.println ("Error : cannot create BufferToImage instance");
                return ( null );
            }
        }else{
            System.out.println ("Error : Buffer grabbed is null");
            return ( null );
        }
    }
 
 
    /* -------------------------------------------------------------------
     * Closes and cleans up the player
     * 플레이어를 닫고 정리합니다.
     * ------------------------------------------------------------------- */
 
    public void playerClose ( ){
        if ( player != null ){
            player.close();
            player.deallocate();
            player = null;
        }
    }
 
    public void windowClosing ( WindowEvent e ){
        playerClose();
        System.exit ( 1 );
    }
 
    public void componentResized ( ComponentEvent e ){
        Dimension dim = getSize();
        boolean mustResize = false;
 
        if ( dim.width < MIN_WIDTH ){
             dim.width = MIN_WIDTH;
             mustResize = true;
        }
 
        if ( dim.height < MIN_HEIGHT ){
            dim.height = MIN_HEIGHT;
            mustResize = true;
        }
 
        if ( mustResize ) {
             setSize ( dim );
        }
    }
 
    public void windowActivated ( WindowEvent e )   {   }
    public void windowClosed ( WindowEvent e )      {   }
    public void windowDeactivated ( WindowEvent e ) {   }
    public void windowDeiconified ( WindowEvent e ) {   }
    public void windowIconified ( WindowEvent e )   {   }
    public void windowOpened ( WindowEvent e )      {   }
    public void componentHidden(ComponentEvent e)   {   }
    public void componentMoved(ComponentEvent e)    {   }
    public void componentShown(ComponentEvent e)    {   }
 
 
    protected void finalize ( ) throws Throwable{
        playerClose();
        super.finalize();
    }
 
    class MyToolBarAction extends AbstractAction{
        public MyToolBarAction ( String name, String imagefile ){
            // Note : Use version this if you supply your own toolbar icons 사용자 고유의 도구 모음 아이콘을 제공하는 경우 버전 사용
            // super (name, new ImageIcon(imagefile));
 
            super (name);
        }
 
        public void actionPerformed (ActionEvent event){
            toolbarHandler (this);
        }
    };
 
 
    class MyVideoFormat{
        public VideoFormat format;
 
        public MyVideoFormat (VideoFormat format){
            this.format = format;
        }
 
        public String toString (){
            Dimension dim = format.getSize();
            return (format.getEncoding() + " [ " + dim.width + " x " + dim.height + " ]");
        }
    };
 
 
    class MyCaptureDeviceInfo{
        public CaptureDeviceInfo capDevInfo;
 
        public MyCaptureDeviceInfo (CaptureDeviceInfo devInfo){
        	
            capDevInfo = devInfo;
        }
 
        public String toString (){
            return (capDevInfo.getName());
        }
    };
 
 
    class MySnapshot extends JFrame implements ImageObserver{
        protected Image photo = null;
        protected int shotNumber;
 
        public MySnapshot (Image grabbedFrame, Dimension imageSize){
            super ( );
 
            shotNumber = shotCounter++;
            setTitle ("Photo" + shotNumber);
 
            photo = grabbedFrame;
 
            setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
 
            int imageHeight = photo.getWidth  (this);
            int imageWidth  = photo.getHeight (this);
 
            setSize (imageSize.width, imageSize.height);
 
            final FileDialog saveDialog = new FileDialog (this, "Save JPEG", FileDialog.SAVE);
            final JFrame thisCopy = this;
            saveDialog.setFile ("Photo" + shotNumber);
 
            addWindowListener (new WindowAdapter(){
                    public void windowClosing (WindowEvent e){
                        saveDialog.show();
 
                        String filename = saveDialog.getFile();
 
                        if (filename != null){
                            if (saveJPEG (filename)){
                                JOptionPane.showMessageDialog(thisCopy, "Saved " + filename);
                                setVisible (false);
                                dispose();
                            }else{
                                JOptionPane.showMessageDialog(thisCopy, "Error saving " + filename);
                            }
                        }else{
                            setVisible (false);
                            dispose();
                        }
                    }
                }
            );
 
            setVisible (true);
        }
 
 
        public void paint (Graphics g){
            super.paint (g);
 
            g.drawImage (photo, 0, 0, getWidth(), getHeight(), Color.black, this);
        }
 
 
 
        /* -------------------------------------------------------------------
         * Saves an image as a JPEG /////////JPGE를 저장
         *
         * @params the image to save
         * @params the filename to save the image as
         * ------------------------------------------------------------------- */
 
        public boolean saveJPEG (String filename){
            boolean saved = false;
            BufferedImage bi = new BufferedImage (photo.getWidth(null),
                                                  photo.getHeight(null),
                                                  BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bi.createGraphics();
            g2.drawImage (photo, null, null );
            FileOutputStream out = null;
 
            try{
                out = new FileOutputStream (filename);
                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder (out);
                JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam (bi);
                param.setQuality (1.0f, false);   // 100% high quality setting, no compression
                encoder.setJPEGEncodeParam (param);
                encoder.encode (bi);
                out.close();
                saved = true;
            }catch(Exception ex){
                System.out.println ("Error saving JPEG : " + ex.getMessage());
            }
 
            return (saved);
        }
 
    }   // of MySnapshot
 
 
 
    public static void main (String[] args){ //메인메소드 실행
        try{        	
        	System.out.println("시작하니?");
            JWebCam myWebCam = new JWebCam("화상 테스트"); 
            myWebCam.setVisible(true); //보여라
 
            if (!myWebCam.initialise()){ //initialise 기본적으로 false
                System.out.println ("Web Cam not detected / initialised");
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}

