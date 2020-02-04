package ac.soton.eventb.internal.scenariochecker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import ac.soton.eventb.statemachines.animation.StatemachineAnimationPlugin;
import de.bmotionstudio.gef.editor.Animation;
import de.bmotionstudio.gef.editor.BMotionEditorPlugin;
import de.bmotionstudio.gef.editor.BMotionStudioEditor;
import de.bmotionstudio.gef.editor.model.Visualization;
import de.prob.core.Animator;

public class BMSStarter {

	private BMSStarter() {} //prevent instantiation
	
	private static boolean running=false;
	private static Map<IFile,BMotionStudioEditor> bmsEditors = new HashMap<IFile,BMotionStudioEditor>();
	
	public static boolean restartBMS(List<IFile> bmsFiles, Animator probAnimator) {
		running = false;
		for (IFile bmsFile : bmsFiles){
			running = running || runBMotionStudio(bmsFile, probAnimator);
		}
		return running;
	}
	
	public static boolean isRunning() {
		return running;
	}
	
/////////////////////	
	
	/**
	 * 
	 * @param bmsFile
	 * @param animator
	 * @return 
	 */
	private static boolean runBMotionStudio(IFile bmsFile, Animator animator){
		try {
			Visualization visualization = createVisualizationRoot(bmsFile);
			Animation animation = new Animation(animator, visualization);
			BMotionStudioEditor bmsEditor = getBmotionStudioEditor(bmsFile);
			bmsEditor.createRunPage(visualization, animation);
			return true;
		} catch (CoreException e) {
			StatemachineAnimationPlugin.logError("Eclipse Core Exception while attempting to launch BMotion Studio", e);
		} catch (IOException e) {
			StatemachineAnimationPlugin.logError("IO Exception while attempting to launch BMotion Studio", e);
		} catch (ParserConfigurationException e) {
			StatemachineAnimationPlugin.logError("Parser Configuration Exception while attempting to launch BMotion Studio", e);
		} catch (SAXException e) {
			StatemachineAnimationPlugin.logError("SAX Exception while attempting to launch BMotion Studio", e);
		}
		return false;
	}
	
	/**
	 * given a BMotionStudio bms file, opens it in an editor and returns the editor
	 * @param bmsFile 
	 * @return BMotionStudioEditor
	 * @throws PartInitException
	 */
	private static BMotionStudioEditor getBmotionStudioEditor(IFile bmsFile) throws PartInitException{
		if (!bmsEditors.containsKey(bmsFile)) {
		    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(bmsFile.getName());
			IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new FileEditorInput(bmsFile), desc.getId());
		    if (part instanceof BMotionStudioEditor) {
		    	bmsEditors.put(bmsFile, (BMotionStudioEditor) part); //return (BMotionStudioEditor) part;
		    }
		}
		return bmsEditors.get(bmsFile);
}
	
	/**
	 * Return a visualisation object for the given BMotion Studio bms file
	 * @param bmsFile 
	 * 
	 * @return Visualization
	 * @throws CoreException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
    private static Visualization createVisualizationRoot(IFile bmsFile) throws CoreException, IOException, ParserConfigurationException, SAXException {
            XStream xstream = new XStream(new DomDriver()) {
                    @Override
                    protected MapperWrapper wrapMapper(MapperWrapper next) {
                            return new MapperWrapper(next) {                      
                            		@Override
                                    public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") Class definedIn,String fieldName) {
                                            if (definedIn == Object.class) {
                                                    return false;
                                            }
                                            return super.shouldSerializeMember(definedIn, fieldName); 
                                    }
                            };
                    }
            };
            BMotionEditorPlugin.setAliases(xstream);
            Visualization visualization = (Visualization) xstream.fromXML(bmsFile.getContents());
            visualization.setProjectFile(bmsFile);
            visualization.setIsRunning(true);
            return visualization;
    }
    
}
