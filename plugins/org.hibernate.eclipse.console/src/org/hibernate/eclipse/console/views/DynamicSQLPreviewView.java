
package org.hibernate.eclipse.console.views;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.console.ConsoleConfiguration;
import org.hibernate.eclipse.console.HibernateConsoleMessages;
import org.hibernate.eclipse.console.common.HibernateExtension;
import org.hibernate.eclipse.hqleditor.HQLEditor;
import org.hibernate.eclipse.hqleditor.HQLEditorDocumentSetupParticipant;
import org.hibernate.eclipse.hqleditor.HQLSourceViewer;
import org.hibernate.eclipse.hqleditor.HQLSourceViewerConfiguration;
import org.hibernate.util.xpl.StringHelper;

public class DynamicSQLPreviewView extends ViewPart {

	private IPartListener2 partListener = new IPartListener2() {

		public void partInputChanged(IWorkbenchPartReference partRef) {

		}

		public void partVisible(IWorkbenchPartReference partRef) {

		}

		public void partHidden(IWorkbenchPartReference partRef) {

		}

		public void partOpened(IWorkbenchPartReference partRef) {

		}

		public void partDeactivated(IWorkbenchPartReference partRef) {

		}

		public void partClosed(IWorkbenchPartReference partRef) {
			if(partRef.getPart(false)==currentEditor) {
				setCurrentEditor(null);
			}
		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) {
			hookIntoEditor(partRef);
		}

		public void partActivated(IWorkbenchPartReference partRef) {
			hookIntoEditor( partRef );
		}



	};

	private SourceViewer textViewer;
	private HQLEditor currentEditor;
    private MonoReconciler reconciler;
    private HQLEditorDocumentSetupParticipant docSetupParticipant = new HQLEditorDocumentSetupParticipant();

    private void hookIntoEditor(IWorkbenchPartReference partRef) {
    	if(partRef==null) {
    		setCurrentEditor(null);
    		return;
    	}
		IWorkbenchPart part = partRef.getPart(false);
		if(part!=null && (part instanceof HQLEditor)) {
			setCurrentEditor((HQLEditor) part);
		}
	}
	private void setCurrentEditor(HQLEditor editor) {
		if(editor==currentEditor) {
			updateText(currentEditor);
			return;
		}
		if(currentEditor!=null) {
			reconciler.uninstall();
		}

		currentEditor = editor;

		if(currentEditor!=null) {
			ITextViewer editorViewer = currentEditor.getTextViewer();
			reconciler.install(editorViewer);
		}

		updateText(currentEditor);

	}

	private void updateText(HQLEditor editor) {
		if(textViewer!=null && textViewer.getDocument()!=null) {
			if(editor!=null) {
				ConsoleConfiguration consoleConfiguration = editor.getConsoleConfiguration();
				if(StringHelper.isEmpty( editor.getQueryString() )) {
					textViewer.getDocument().set( HibernateConsoleMessages.DynamicSQLPreviewView_empty_hql_query );
				} else if(consoleConfiguration!=null) {
					HibernateExtension hibernateExtension = consoleConfiguration.getHibernateExtension();
					if(hibernateExtension.isSessionFactoryCreated()) {
						String generateSQL = hibernateExtension.generateSQL(editor.getQueryString());
						if(StringHelper.isEmpty( generateSQL )) {
							textViewer.getDocument().set( HibernateConsoleMessages.DynamicSQLPreviewView_no_sql_generated );
						} else {
							textViewer.getDocument().set(generateSQL);
						}
					} else {
						textViewer.getDocument().set(HibernateConsoleMessages.DynamicSQLPreviewView_session_factory_not_created + consoleConfiguration.getName());
					}
				} else {
					textViewer.getDocument().set(HibernateConsoleMessages.DynamicSQLPreviewView_no_console_conf_associated);
				}
			} else {
				textViewer.getDocument().set(HibernateConsoleMessages.DynamicSQLPreviewView_no_hql_query_editor);
			}
		}
	}

	public void createPartControl(Composite parent) {
		textViewer = new HQLSourceViewer( parent, new VerticalRuler(1), null, false, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL );
		//textViewer.setEditable(false);
		IDocument doc = new Document();
		textViewer.setDocument( doc );

		docSetupParticipant.setup( doc );

		textViewer.getDocument().set(HibernateConsoleMessages.DynamicSQLPreviewView_no_hql_query_editor_selected);
		textViewer.configure(new HQLSourceViewerConfiguration(null));

		IWorkbenchWindow window = PlatformUI.getWorkbench()
		.getActiveWorkbenchWindow();
		IPartService service = window.getPartService();

		hookIntoEditor(service.getActivePartReference());
	}

	public void init(IViewSite site) throws PartInitException {
		super.init(site);

		IReconcilingStrategy strategy = new AbstractReconcilingStrategy() {

			protected void doReconcile(final IDocument doc) {
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.asyncExec(new Runnable() {

					public void run() {
						//textViewer.getDocument().set(doc.get());
						updateText(currentEditor);
					}
				});
			}

		};

		reconciler = new MonoReconciler(strategy,false);
		reconciler.setDelay(500);

		IWorkbenchWindow window = PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow();
		IPartService service = window.getPartService();
		service.addPartListener(partListener);

		hookIntoEditor(service.getActivePartReference());

	}

	public void dispose() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow();
		if (window != null){
			IPartService service = window.getPartService();
			service.removePartListener(partListener);
		}
		docSetupParticipant.unsetup();
		super.dispose();
	}

	public void setFocus() {

	}
}
