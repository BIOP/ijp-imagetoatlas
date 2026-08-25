# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ABBA (Aligning Big Brains & Atlases) is an ImageJ/Fiji plugin for aligning 2D brain slice images with 3D brain atlases (Allen Brain Atlas). The core Java package provides registration, visualization, and state management capabilities.

**Associated Repositories:**
- Documentation: https://github.com/BIOP/abba-documentation
- QuPath extension: https://github.com/BIOP/qupath-extension-abba
- Python wrapper: https://github.com/BIOP/abba_python
- Windows installer: https://github.com/BIOP/abba-installer

## Build and Test Commands

### Building
```bash
# Standard Maven build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build for a specific Fiji installation (uncomment scijava.app.directory in pom.xml)
mvn clean install -Dscijava.app.directory=C:/Fiji
```

### Testing
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ABBALaunchMouse

# Run benchmark (requires OMERO setup)
mvn test -Dtest=ABBABenchMarkCommand
```

The `src/test/java` directory contains demo launchers and plugin examples rather than traditional unit tests. Key test files:
- `ABBALaunchMouse.java`, `ABBALaunchRat.java` - Launch ABBA with demo atlases
- `pluginexample/` - Examples for extending ABBA with custom registration methods
- `ScijavaCommandToPython.java` - Generates Python API bindings

### CI Build
The project uses SciJava's CI scripts:
- `.github/build.sh` downloads and executes `ci-build.sh` from scijava-scripts
- CI builds on Java 21 (Zulu distribution)
- Deploys to SciJava Maven repository on tagged releases

## Architecture Overview

### Core Architectural Pattern

ABBA uses a three-layer architecture with an action-based state management system:

1. **Model Layer** - Core domain logic
   - `MultiSlicePositioner` - Central orchestrator managing the entire registration workflow
   - `SliceSources` - Represents individual slice images with transformation states
   - `ReslicedAtlas` - Manages dynamic 3D atlas reslicing based on user orientation
   - `CancelableAction` - Base class for all state-changing operations (undo/redo support)

2. **Command/View Layer** - User interface
   - 64 SciJava Command plugins exposing functionality
   - BigDataViewer (BDV) integration for 3D visualization
   - Card-based UI panels (AtlasInfoPanel, EditPanel, NavigationPanel)

3. **Adapter/Serialization Layer** - Persistence
   - Gson-based JSON serialization with custom type adapters
   - `AlignerState` captures complete state snapshots
   - Legacy format compatibility handling

### Key Packages

```
ch.epfl.biop.atlas.aligner/
├── [root]          - Core model classes (MultiSlicePositioner, SliceSources, ReslicedAtlas, *Action)
├── adapter/        - GSON JSON serialization adapters (AlignerState, RegisterSliceAdapter, etc.)
├── action/         - Concrete action implementations (export, raster, key frames)
├── command/        - SciJava commands (64 total) exposing functionality to UI
├── gui/            - UI components and interaction handlers
│   └── bdv/        - BigDataViewer window management, behaviors, overlays
│       └── card/   - UI panels (navigation, editing, info)
├── processor/      - SciJava module processing hooks
└── plugin/         - Extension points for third-party commands

ch.epfl.biop.quicknii/ - QuickNII format import/export
ch.epfl.biop/          - Utility classes (ResourcesMonitor)
```

### Registration Workflow

The system supports multiple registration backends through a plugin architecture:

1. **Elastix-based** - Affine and B-spline deformable registration
2. **DeepSlice** - Deep learning-based (local or web service)
3. **BigWarp** - Manual/interactive landmark-based
4. **QuickNII** - Import pre-existing registrations

Registration flow:
```
User selects slices → RegisterSlices*Command.run()
  → MultiSlicePositioner.registerSelectedSlices()
  → RegisterSliceAction (one per slice)
  → Preprocessing pipeline (channel selection, resampling, filtering)
  → Registration plugin execution
  → Transform stored in SliceSources
  → BDV updates visualization
```

Each `RegisterSliceAction` contains:
- Reference to registration plugin (e.g., Elastix2DAffineRegistration)
- Preprocessing pipeline (SourcesProcessor chain)
- Registration result (AffineTransform3D or deformation field)
- State tracking (pending, running, done, locked, invalid)

### State Persistence

State is saved as ZIP archives containing:
- `metadata.json` - AlignerState with atlas orientation, slice list, action history
- Source references and preprocessing pipeline configurations

**Action filtering during serialization:**
- **Saved**: CreateSlice, MoveSlice, RegisterSlice, KeySliceOn/Off, UnMirror, SetBackground
- **Omitted**: Export actions, temporary edits, failed registrations

Custom GSON adapters handle serialization:
- `RegisterSliceAdapter` - Serializes registration transforms only, not preprocessing artifacts
- `CreateSliceAdapter` - Preserves slice source references
- `SliceSourcesStateDeserializer` - Reconstructs action history from JSON
- Legacy class name conversion for backward compatibility

### BigDataViewer Integration

BDV serves as the visualization engine:
- **Sources**: Each slice channel and atlas is a `SourceAndConverter`
- **Behaviors**: Custom mouse/keyboard handlers (SliceDragBehaviour, MultiSliceContextMenuClickBehaviour)
- **Overlays**: Custom rendering for selection layer, action timeline, registration status
- **Transformations**: All slice positioning/registration uses BDV's transformation system

`BdvMultislicePositionerView` orchestrates the BDV window, panels, and interactions.

### Key Design Patterns

| Pattern | Usage | Location |
|---------|-------|----------|
| **Observer** | Listen to slice state changes | `SliceActionObserver`, `SliceChangeListener` |
| **Command** | Undo/redo support | `CancelableAction` hierarchy |
| **Adapter** | Custom JSON serialization | `adapter/` package |
| **Plugin** | Extensibility for registration methods | `IRegistrationPlugin`, `ABBACommand` interface |
| **Factory** | Create slices and actions | `MultiSlicePositioner.createSlice()` |
| **Strategy** | Pluggable registration backends | Different `Registration` implementations |

### Threading and Concurrency

- `MultiSlicePositioner.manualActionLock` - Synchronizes operations requiring user feedback
- `CopyOnWriteArrayList<SliceSources>` - Thread-safe slice collection
- Async registration execution via `action.runRequest(true)` for background processing
- UI stays responsive during long-running registrations

## Extension Points

### Adding Custom Registration Method

1. Implement `Registration<SourceAndConverter<?>[]>` from `ch.epfl.biop.registration` package
2. Create a SciJava Command extending `RegistrationMultiChannelCommand` or `RegistrationSingleChannelCommand`
3. The command will automatically appear in ABBA's registration menu

Example: `src/test/java/ch/epfl/biop/abba/pluginexample/`

### Adding Custom Slice Operations

1. Extend `CancelableAction`
2. Implement `run()` and `cancel()` methods
3. Queue execution via `action.runRequest()`

Example: `src/test/java/ch/epfl/biop/abba/actionexample/PrintTheNumberOfRoisAction.java`

### Adding Custom Export Format

1. Create a Command taking `MultiSlicePositioner` as input parameter
2. Access transformation data via `AlignerState` or directly from slices
3. Implement export logic

Example: `ExportSlicesToQuickNIIDatasetCommand.java`

## Important Implementation Notes

### Atlas Reslicing

`ReslicedAtlas` handles dynamic reslicing perpendicular to any orientation:
- Supports three rotation parameters (X angle, Y angle, center point)
- Computes bounding boxes to determine slice extent
- Maintains extended versions (15% margin for tilt angle safety)
- Updates all atlas sources when orientation changes via `setSlicingTransform()`

### Slice Representation

Each `SliceSources` contains:
- **originalSources[]** - Raw multi-channel image data
- **registeredSources[]** - Transformed versions after registration
- **actionsPerformed** - History of all modifications (moves, registrations)
- **actionSequence** - Sequential index for timeline visualization
- Position, thickness, background value metadata

### Data Dependencies

**Core Framework:**
- SciJava - Command discovery, dependency injection, context management
- ImageJ/Fiji - Image processing foundation
- BigDataViewer - Visualization engine
- ImgLib2 - Image data structures and transformations

**Specialized BIOP Libraries:**
- `ch.epfl.biop.registration` - Registration plugin interfaces
- `ch.epfl.biop.atlas` - Brain atlas structures
- `ch.epfl.biop.bdv-playground` - Advanced BDV extensions
- `ch.epfl.biop.bigdataviewer-biop-tools` - BDV utilities
- `ch.epfl.biop.bigdataviewer-image-loaders` - Image loading
- `ch.epfl.biop.bigdataviewer-selector` - Source selection

**Key Versions (see pom.xml):**
- Java 21 target (`scijava.jvm.version`)
- bigdataviewer-playground: 0.13.0
- atlas: 0.3.2

### SciJava Command Organization

Commands are organized by functional area:
- **Workflow**: ABBAStartCommand, Import*, Export*
- **Registration**: RegisterSlices*Command (Elastix, DeepSlice, BigWarp)
- **State**: ABBAStateSaveCommand, ABBAStateLoadCommand
- **Manipulation**: SetSlices*, RotateSlicesCommand, MirrorDoCommand
- **Utility**: ABBADocumentationCommand, ABBACheckForUpdateCommand

All commands are auto-discovered via SciJava's plugin mechanism.

## Common Development Patterns

### Accessing MultiSlicePositioner from Commands

```java
@Plugin(type = Command.class, menuPath = "Plugins>ABBA>My Command")
public class MyCommand implements Command {
    @Parameter
    MultiSlicePositioner mp;

    @Override
    public void run() {
        // Access slices, atlas, perform operations
        mp.getSlices().forEach(slice -> { /* ... */ });
    }
}
```

### Creating and Running Actions

```java
CreateSliceAction action = new CreateSliceAction(mp, sources);
action.runRequest(); // Synchronous
// or
action.runRequest(true); // Asynchronous with callback
```

### Listening to Slice Changes

```java
mp.addSliceActionObserver(new SliceActionObserver() {
    @Override
    public void sliceCreated(SliceSources slice) { /* ... */ }
    @Override
    public void sliceDeleted(SliceSources slice) { /* ... */ }
    @Override
    public void sliceSourcesChanged(SliceSources slice) { /* ... */ }
});
```

### Custom GSON Adapter Registration

See `MultiSlicePositioner.getGson()` method for examples of registering custom type adapters using `RuntimeTypeAdapterFactory`.

## Documentation and Support

- Full documentation: https://abba-documentation.readthedocs.io
- Forum support: https://forum.image.sc/ (tag: abba)
- Issue tracker: https://github.com/BIOP/ijp-imagetoatlas/issues
