# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

add_subdirectory( crash )
add_subdirectory( fonthelper )

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
set(CELESTIA_DIR ${CMAKE_SOURCE_DIR}/../../../../../Celestia)
set(CELESTIA_SRC_DIR "${CELESTIA_DIR}/src")
set(CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celestia/celestiacore.cpp
        ${CELESTIA_SRC_DIR}/celestia/celestiacore.h
        ${CELESTIA_SRC_DIR}/celestia/configfile.cpp
        ${CELESTIA_SRC_DIR}/celestia/configfile.h
        ${CELESTIA_SRC_DIR}/celestia/destination.cpp
        ${CELESTIA_SRC_DIR}/celestia/destination.h
        ${CELESTIA_SRC_DIR}/celestia/eclipsefinder.cpp
        ${CELESTIA_SRC_DIR}/celestia/eclipsefinder.h
        ${CELESTIA_SRC_DIR}/celestia/favorites.cpp
        ${CELESTIA_SRC_DIR}/celestia/favorites.h
        ${CELESTIA_SRC_DIR}/celestia/helper.cpp
        ${CELESTIA_SRC_DIR}/celestia/helper.h
        ${CELESTIA_SRC_DIR}/celestia/imagecapture.cpp
        ${CELESTIA_SRC_DIR}/celestia/imagecapture.h
        ${CELESTIA_SRC_DIR}/celestia/moviecapture.h
        ${CELESTIA_SRC_DIR}/celestia/scriptmenu.cpp
        ${CELESTIA_SRC_DIR}/celestia/scriptmenu.h
        ${CELESTIA_SRC_DIR}/celestia/url.cpp
        ${CELESTIA_SRC_DIR}/celestia/url.h
        ${CELESTIA_SRC_DIR}/celestia/view.cpp
        ${CELESTIA_SRC_DIR}/celestia/view.h
        ${CELESTIA_SRC_DIR}/celestia/oggtheoracapture.cpp
        ${CELESTIA_SRC_DIR}/celestia/oggtheoracapture.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celttf/truetypefont.cpp
        ${CELESTIA_SRC_DIR}/celttf/truetypefont.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celutil/bigfix.cpp
        ${CELESTIA_SRC_DIR}/celutil/bigfix.h
        ${CELESTIA_SRC_DIR}/celutil/blockarray.h
        ${CELESTIA_SRC_DIR}/celutil/bytes.h
        ${CELESTIA_SRC_DIR}/celutil/color.cpp
        ${CELESTIA_SRC_DIR}/celutil/color.h
        ${CELESTIA_SRC_DIR}/celutil/debug.cpp
        ${CELESTIA_SRC_DIR}/celutil/debug.h
        ${CELESTIA_SRC_DIR}/celutil/filetype.cpp
        ${CELESTIA_SRC_DIR}/celutil/filetype.h
        ${CELESTIA_SRC_DIR}/celutil/formatnum.cpp
        ${CELESTIA_SRC_DIR}/celutil/formatnum.h
        ${CELESTIA_SRC_DIR}/celutil/memorypool.cpp
        ${CELESTIA_SRC_DIR}/celutil/memorypool.h
        ${CELESTIA_SRC_DIR}/celutil/reshandle.h
        ${CELESTIA_SRC_DIR}/celutil/resmanager.h
        ${CELESTIA_SRC_DIR}/celutil/strnatcmp.h
        ${CELESTIA_SRC_DIR}/celutil/strnatcmp.cpp
        ${CELESTIA_SRC_DIR}/celutil/timer.cpp
        ${CELESTIA_SRC_DIR}/celutil/timer.h
        ${CELESTIA_SRC_DIR}/celutil/utf8.cpp
        ${CELESTIA_SRC_DIR}/celutil/utf8.h
        ${CELESTIA_SRC_DIR}/celutil/util.cpp
        ${CELESTIA_SRC_DIR}/celutil/util.h
        ${CELESTIA_SRC_DIR}/celutil/watcher.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celutil/bigfix.cpp
        ${CELESTIA_SRC_DIR}/celutil/bigfix.h
        ${CELESTIA_SRC_DIR}/celutil/blockarray.h
        ${CELESTIA_SRC_DIR}/celutil/bytes.h
        ${CELESTIA_SRC_DIR}/celutil/color.cpp
        ${CELESTIA_SRC_DIR}/celutil/color.h
        ${CELESTIA_SRC_DIR}/celutil/debug.cpp
        ${CELESTIA_SRC_DIR}/celutil/debug.h
        ${CELESTIA_SRC_DIR}/celutil/filetype.cpp
        ${CELESTIA_SRC_DIR}/celutil/filetype.h
        ${CELESTIA_SRC_DIR}/celutil/formatnum.cpp
        ${CELESTIA_SRC_DIR}/celutil/formatnum.h
        ${CELESTIA_SRC_DIR}/celutil/memorypool.cpp
        ${CELESTIA_SRC_DIR}/celutil/memorypool.h
        ${CELESTIA_SRC_DIR}/celutil/reshandle.h
        ${CELESTIA_SRC_DIR}/celutil/resmanager.h
        ${CELESTIA_SRC_DIR}/celutil/timer.cpp
        ${CELESTIA_SRC_DIR}/celutil/timer.h
        ${CELESTIA_SRC_DIR}/celutil/utf8.cpp
        ${CELESTIA_SRC_DIR}/celutil/utf8.h
        ${CELESTIA_SRC_DIR}/celutil/util.cpp
        ${CELESTIA_SRC_DIR}/celutil/util.h
        ${CELESTIA_SRC_DIR}/celutil/watcher.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celscript/common/script.cpp
        ${CELESTIA_SRC_DIR}/celscript/common/script.h
        ${CELESTIA_SRC_DIR}/celscript/common/scriptmaps.cpp
        ${CELESTIA_SRC_DIR}/celscript/common/scriptmaps.h
        ${CELESTIA_SRC_DIR}/celscript/legacy/cmdparser.cpp
        ${CELESTIA_SRC_DIR}/celscript/legacy/cmdparser.h
        ${CELESTIA_SRC_DIR}/celscript/legacy/command.cpp
        ${CELESTIA_SRC_DIR}/celscript/legacy/command.h
        ${CELESTIA_SRC_DIR}/celscript/legacy/execenv.h
        ${CELESTIA_SRC_DIR}/celscript/legacy/execution.cpp
        ${CELESTIA_SRC_DIR}/celscript/legacy/execution.h
        ${CELESTIA_SRC_DIR}/celscript/legacy/legacyscript.cpp
        ${CELESTIA_SRC_DIR}/celscript/legacy/legacyscript.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_category.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_category.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_celestia.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_celestia.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_frame.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_frame.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_internal.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_misc.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_misc.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_object.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_object.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_observer.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_observer.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_phase.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_phase.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_position.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_position.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_rotation.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_rotation.h
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_vector.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/celx_vector.h
        ${CELESTIA_SRC_DIR}/celscript/lua/luascript.cpp
        ${CELESTIA_SRC_DIR}/celscript/lua/luascript.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celcompat/fs.cpp
        ${CELESTIA_SRC_DIR}/celcompat/fs.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celmodel/material.cpp
        ${CELESTIA_SRC_DIR}/celmodel/material.h
        ${CELESTIA_SRC_DIR}/celmodel/mesh.cpp
        ${CELESTIA_SRC_DIR}/celmodel/mesh.h
        ${CELESTIA_SRC_DIR}/celmodel/model.cpp
        ${CELESTIA_SRC_DIR}/celmodel/modelfile.cpp
        ${CELESTIA_SRC_DIR}/celmodel/modelfile.h
        ${CELESTIA_SRC_DIR}/celmodel/model.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celephem/customorbit.cpp
        ${CELESTIA_SRC_DIR}/celephem/customorbit.h
        ${CELESTIA_SRC_DIR}/celephem/customrotation.cpp
        ${CELESTIA_SRC_DIR}/celephem/customrotation.h
        ${CELESTIA_SRC_DIR}/celephem/jpleph.cpp
        ${CELESTIA_SRC_DIR}/celephem/jpleph.h
        ${CELESTIA_SRC_DIR}/celephem/nutation.cpp
        ${CELESTIA_SRC_DIR}/celephem/nutation.h
        ${CELESTIA_SRC_DIR}/celephem/orbit.cpp
        ${CELESTIA_SRC_DIR}/celephem/orbit.h
        ${CELESTIA_SRC_DIR}/celephem/precession.cpp
        ${CELESTIA_SRC_DIR}/celephem/precession.h
        ${CELESTIA_SRC_DIR}/celephem/rotation.cpp
        ${CELESTIA_SRC_DIR}/celephem/rotation.h
        ${CELESTIA_SRC_DIR}/celephem/samporbit.cpp
        ${CELESTIA_SRC_DIR}/celephem/samporbit.h
        ${CELESTIA_SRC_DIR}/celephem/samporient.cpp
        ${CELESTIA_SRC_DIR}/celephem/samporient.h
        ${CELESTIA_SRC_DIR}/celephem/vsop87.cpp
        ${CELESTIA_SRC_DIR}/celephem/vsop87.h
        ${CELESTIA_SRC_DIR}/celephem/scriptobject.cpp
        ${CELESTIA_SRC_DIR}/celephem/scriptobject.h
        ${CELESTIA_SRC_DIR}/celephem/scriptorbit.cpp
        ${CELESTIA_SRC_DIR}/celephem/scriptorbit.h
        ${CELESTIA_SRC_DIR}/celephem/scriptrotation.cpp
        ${CELESTIA_SRC_DIR}/celephem/scriptrotation.h
        ${CELESTIA_SRC_DIR}/celephem/spiceinterface.cpp
        ${CELESTIA_SRC_DIR}/celephem/spiceinterface.h
        ${CELESTIA_SRC_DIR}/celephem/spiceorbit.cpp
        ${CELESTIA_SRC_DIR}/celephem/spiceorbit.h
        ${CELESTIA_SRC_DIR}/celephem/spicerotation.cpp
        ${CELESTIA_SRC_DIR}/celephem/spicerotation.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celengine/asterism.cpp
        ${CELESTIA_SRC_DIR}/celengine/asterism.h
        ${CELESTIA_SRC_DIR}/celengine/asterismrenderer.cpp
        ${CELESTIA_SRC_DIR}/celengine/asterismrenderer.h
        ${CELESTIA_SRC_DIR}/celengine/astro.cpp
        ${CELESTIA_SRC_DIR}/celengine/astro.h
        ${CELESTIA_SRC_DIR}/celengine/astroobj.h
        ${CELESTIA_SRC_DIR}/celengine/astroobj.cpp
        ${CELESTIA_SRC_DIR}/celengine/atmosphere.h
        ${CELESTIA_SRC_DIR}/celengine/axisarrow.cpp
        ${CELESTIA_SRC_DIR}/celengine/axisarrow.h
        ${CELESTIA_SRC_DIR}/celengine/body.cpp
        ${CELESTIA_SRC_DIR}/celengine/body.h
        ${CELESTIA_SRC_DIR}/celengine/boundaries.cpp
        ${CELESTIA_SRC_DIR}/celengine/boundaries.h
        ${CELESTIA_SRC_DIR}/celengine/boundariesrenderer.cpp
        ${CELESTIA_SRC_DIR}/celengine/boundariesrenderer.h
        ${CELESTIA_SRC_DIR}/celengine/catalogxref.cpp
        ${CELESTIA_SRC_DIR}/celengine/catalogxref.h
        ${CELESTIA_SRC_DIR}/celengine/category.cpp
        ${CELESTIA_SRC_DIR}/celengine/category.h
        ${CELESTIA_SRC_DIR}/celengine/console.cpp
        ${CELESTIA_SRC_DIR}/celengine/console.h
        ${CELESTIA_SRC_DIR}/celengine/constellation.cpp
        ${CELESTIA_SRC_DIR}/celengine/constellation.h
        ${CELESTIA_SRC_DIR}/celengine/curveplot.cpp
        ${CELESTIA_SRC_DIR}/celengine/curveplot.h
        ${CELESTIA_SRC_DIR}/celengine/dds.cpp
        ${CELESTIA_SRC_DIR}/celengine/dds_decompress.c
        ${CELESTIA_SRC_DIR}/celengine/dds_decompress.h
        ${CELESTIA_SRC_DIR}/celengine/deepskyobj.cpp
        ${CELESTIA_SRC_DIR}/celengine/deepskyobj.h
        ${CELESTIA_SRC_DIR}/celengine/dispmap.cpp
        ${CELESTIA_SRC_DIR}/celengine/dispmap.h
        ${CELESTIA_SRC_DIR}/celengine/dsodb.cpp
        ${CELESTIA_SRC_DIR}/celengine/dsodb.h
        ${CELESTIA_SRC_DIR}/celengine/dsoname.cpp
        ${CELESTIA_SRC_DIR}/celengine/dsoname.h
        ${CELESTIA_SRC_DIR}/celengine/dsooctree.cpp
        ${CELESTIA_SRC_DIR}/celengine/dsooctree.h
        ${CELESTIA_SRC_DIR}/celengine/dsorenderer.cpp
        ${CELESTIA_SRC_DIR}/celengine/dsorenderer.h
        ${CELESTIA_SRC_DIR}/celengine/frame.cpp
        ${CELESTIA_SRC_DIR}/celengine/frame.h
        ${CELESTIA_SRC_DIR}/celengine/framebuffer.cpp
        ${CELESTIA_SRC_DIR}/celengine/framebuffer.h
        ${CELESTIA_SRC_DIR}/celengine/frametree.cpp
        ${CELESTIA_SRC_DIR}/celengine/frametree.h
        ${CELESTIA_SRC_DIR}/celengine/galaxy.cpp
        ${CELESTIA_SRC_DIR}/celengine/galaxy.h
        ${CELESTIA_SRC_DIR}/celengine/geometry.h
        ${CELESTIA_SRC_DIR}/celengine/glsupport.cpp
        ${CELESTIA_SRC_DIR}/celengine/glsupport.h
        ${CELESTIA_SRC_DIR}/celengine/glmarker.cpp
        ${CELESTIA_SRC_DIR}/celengine/globular.cpp
        ${CELESTIA_SRC_DIR}/celengine/globular.h
        ${CELESTIA_SRC_DIR}/celengine/glshader.cpp
        ${CELESTIA_SRC_DIR}/celengine/glshader.h
        ${CELESTIA_SRC_DIR}/celengine/hash.cpp
        ${CELESTIA_SRC_DIR}/celengine/hash.h
        ${CELESTIA_SRC_DIR}/celengine/image.cpp
        ${CELESTIA_SRC_DIR}/celengine/image.h
        ${CELESTIA_SRC_DIR}/celengine/lightenv.h
        ${CELESTIA_SRC_DIR}/celengine/location.cpp
        ${CELESTIA_SRC_DIR}/celengine/location.h
        ${CELESTIA_SRC_DIR}/celengine/lodspheremesh.cpp
        ${CELESTIA_SRC_DIR}/celengine/lodspheremesh.h
        ${CELESTIA_SRC_DIR}/celengine/marker.cpp
        ${CELESTIA_SRC_DIR}/celengine/marker.h
        ${CELESTIA_SRC_DIR}/celengine/meshmanager.cpp
        ${CELESTIA_SRC_DIR}/celengine/meshmanager.h
        ${CELESTIA_SRC_DIR}/celengine/mapmanager.cpp
        ${CELESTIA_SRC_DIR}/celengine/mapmanager.h
        ${CELESTIA_SRC_DIR}/celengine/modelgeometry.cpp
        ${CELESTIA_SRC_DIR}/celengine/modelgeometry.h
        ${CELESTIA_SRC_DIR}/celengine/multitexture.cpp
        ${CELESTIA_SRC_DIR}/celengine/multitexture.h
        ${CELESTIA_SRC_DIR}/celengine/name.cpp
        ${CELESTIA_SRC_DIR}/celengine/name.h
        ${CELESTIA_SRC_DIR}/celengine/nebula.cpp
        ${CELESTIA_SRC_DIR}/celengine/nebula.h
        ${CELESTIA_SRC_DIR}/celengine/objectrenderer.h
        ${CELESTIA_SRC_DIR}/celengine/observer.cpp
        ${CELESTIA_SRC_DIR}/celengine/observer.h
        ${CELESTIA_SRC_DIR}/celengine/octree.h
        ${CELESTIA_SRC_DIR}/celengine/opencluster.cpp
        ${CELESTIA_SRC_DIR}/celengine/opencluster.h
        ${CELESTIA_SRC_DIR}/celengine/orbitsampler.h
        ${CELESTIA_SRC_DIR}/celengine/overlay.cpp
        ${CELESTIA_SRC_DIR}/celengine/overlay.h
        ${CELESTIA_SRC_DIR}/celengine/overlayimage.cpp
        ${CELESTIA_SRC_DIR}/celengine/overlayimage.h
        ${CELESTIA_SRC_DIR}/celengine/parseobject.cpp
        ${CELESTIA_SRC_DIR}/celengine/parseobject.h
        ${CELESTIA_SRC_DIR}/celengine/parser.cpp
        ${CELESTIA_SRC_DIR}/celengine/parser.h
        ${CELESTIA_SRC_DIR}/celengine/planetgrid.cpp
        ${CELESTIA_SRC_DIR}/celengine/planetgrid.h
        ${CELESTIA_SRC_DIR}/celengine/pointstarrenderer.cpp
        ${CELESTIA_SRC_DIR}/celengine/pointstarrenderer.h
        ${CELESTIA_SRC_DIR}/celengine/pointstarvertexbuffer.cpp
        ${CELESTIA_SRC_DIR}/celengine/pointstarvertexbuffer.h
        ${CELESTIA_SRC_DIR}/celengine/rectangle.h
        ${CELESTIA_SRC_DIR}/celengine/referencemark.h
        ${CELESTIA_SRC_DIR}/celengine/rendcontext.cpp
        ${CELESTIA_SRC_DIR}/celengine/rendcontext.h
        ${CELESTIA_SRC_DIR}/celengine/render.cpp
        ${CELESTIA_SRC_DIR}/celengine/render.h
        ${CELESTIA_SRC_DIR}/celengine/renderglsl.cpp
        ${CELESTIA_SRC_DIR}/celengine/renderglsl.h
        ${CELESTIA_SRC_DIR}/celengine/renderinfo.h
        ${CELESTIA_SRC_DIR}/celengine/renderlistentry.h
        ${CELESTIA_SRC_DIR}/celengine/rotationmanager.cpp
        ${CELESTIA_SRC_DIR}/celengine/rotationmanager.h
        ${CELESTIA_SRC_DIR}/celengine/selection.cpp
        ${CELESTIA_SRC_DIR}/celengine/selection.h
        ${CELESTIA_SRC_DIR}/celengine/shadermanager.cpp
        ${CELESTIA_SRC_DIR}/celengine/shadermanager.h
        ${CELESTIA_SRC_DIR}/celengine/shared.h
        ${CELESTIA_SRC_DIR}/celengine/simulation.cpp
        ${CELESTIA_SRC_DIR}/celengine/simulation.h
        ${CELESTIA_SRC_DIR}/celengine/skygrid.cpp
        ${CELESTIA_SRC_DIR}/celengine/skygrid.h
        ${CELESTIA_SRC_DIR}/celengine/solarsys.cpp
        ${CELESTIA_SRC_DIR}/celengine/solarsys.h
        ${CELESTIA_SRC_DIR}/celengine/spheremesh.cpp
        ${CELESTIA_SRC_DIR}/celengine/spheremesh.h
        ${CELESTIA_SRC_DIR}/celengine/starbrowser.cpp
        ${CELESTIA_SRC_DIR}/celengine/starbrowser.h
        ${CELESTIA_SRC_DIR}/celengine/starcolors.cpp
        ${CELESTIA_SRC_DIR}/celengine/starcolors.h
        ${CELESTIA_SRC_DIR}/celengine/star.cpp
        ${CELESTIA_SRC_DIR}/celengine/star.h
        ${CELESTIA_SRC_DIR}/celengine/stardb.cpp
        ${CELESTIA_SRC_DIR}/celengine/stardb.h
        ${CELESTIA_SRC_DIR}/celengine/starname.cpp
        ${CELESTIA_SRC_DIR}/celengine/starname.h
        ${CELESTIA_SRC_DIR}/celengine/staroctree.cpp
        ${CELESTIA_SRC_DIR}/celengine/staroctree.h
        ${CELESTIA_SRC_DIR}/celengine/stb_dxt_104.h
        ${CELESTIA_SRC_DIR}/celengine/stellarclass.cpp
        ${CELESTIA_SRC_DIR}/celengine/stellarclass.h
        ${CELESTIA_SRC_DIR}/celengine/surface.h
        ${CELESTIA_SRC_DIR}/celengine/texmanager.cpp
        ${CELESTIA_SRC_DIR}/celengine/texmanager.h
        ${CELESTIA_SRC_DIR}/celengine/texture.cpp
        ${CELESTIA_SRC_DIR}/celengine/texture.h
        ${CELESTIA_SRC_DIR}/celengine/timeline.cpp
        ${CELESTIA_SRC_DIR}/celengine/timeline.h
        ${CELESTIA_SRC_DIR}/celengine/timelinephase.cpp
        ${CELESTIA_SRC_DIR}/celengine/timelinephase.h
        ${CELESTIA_SRC_DIR}/celengine/tokenizer.cpp
        ${CELESTIA_SRC_DIR}/celengine/tokenizer.h
        ${CELESTIA_SRC_DIR}/celengine/trajmanager.cpp
        ${CELESTIA_SRC_DIR}/celengine/trajmanager.h
        ${CELESTIA_SRC_DIR}/celengine/univcoord.cpp
        ${CELESTIA_SRC_DIR}/celengine/univcoord.h
        ${CELESTIA_SRC_DIR}/celengine/universe.cpp
        ${CELESTIA_SRC_DIR}/celengine/universe.h
        ${CELESTIA_SRC_DIR}/celengine/value.cpp
        ${CELESTIA_SRC_DIR}/celengine/value.h
        ${CELESTIA_SRC_DIR}/celengine/vecgl.h
        ${CELESTIA_SRC_DIR}/celengine/vertexobject.cpp
        ${CELESTIA_SRC_DIR}/celengine/vertexobject.h
        ${CELESTIA_SRC_DIR}/celengine/viewporteffect.cpp
        ${CELESTIA_SRC_DIR}/celengine/viewporteffect.h
        ${CELESTIA_SRC_DIR}/celengine/virtualtex.cpp
        ${CELESTIA_SRC_DIR}/celengine/virtualtex.h
        ${CELESTIA_SRC_DIR}/celengine/visibleregion.cpp
        ${CELESTIA_SRC_DIR}/celengine/visibleregion.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/cel3ds/3dschunk.h
        ${CELESTIA_SRC_DIR}/cel3ds/3dsmodel.cpp
        ${CELESTIA_SRC_DIR}/cel3ds/3dsmodel.h
        ${CELESTIA_SRC_DIR}/cel3ds/3dsread.cpp
        ${CELESTIA_SRC_DIR}/cel3ds/3dsread.h
        )

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_SRC_DIR}/celmath/distance.h
        ${CELESTIA_SRC_DIR}/celmath/ellipsoid.h
        ${CELESTIA_SRC_DIR}/celmath/frustum.cpp
        ${CELESTIA_SRC_DIR}/celmath/frustum.h
        ${CELESTIA_SRC_DIR}/celmath/geomutil.h
        ${CELESTIA_SRC_DIR}/celmath/intersect.h
        ${CELESTIA_SRC_DIR}/celmath/mathlib.h
        ${CELESTIA_SRC_DIR}/celmath/perlin.cpp
        ${CELESTIA_SRC_DIR}/celmath/perlin.h
        ${CELESTIA_SRC_DIR}/celmath/ray.h
        ${CELESTIA_SRC_DIR}/celmath/solve.h
        ${CELESTIA_SRC_DIR}/celmath/sphere.h
        )


set(CELESTIA_JNI_DIR ${CMAKE_SOURCE_DIR})

list(APPEND CELESTIA_SOURCES
        ${CELESTIA_JNI_DIR}/CelestiaAppCore.cpp
        ${CELESTIA_JNI_DIR}/CelestiaSimulation.cpp
        ${CELESTIA_JNI_DIR}/CelestiaSelection.cpp
        ${CELESTIA_JNI_DIR}/CelestiaAstroObject.cpp
        ${CELESTIA_JNI_DIR}/CelestiaUniverse.cpp
        ${CELESTIA_JNI_DIR}/CelestiaStarCatalog.cpp
        ${CELESTIA_JNI_DIR}/CelestiaDSOCatalog.cpp
        ${CELESTIA_JNI_DIR}/CelestiaDSO.cpp
        ${CELESTIA_JNI_DIR}/CelestiaStar.cpp
        ${CELESTIA_JNI_DIR}/CelestiaBody.cpp
        ${CELESTIA_JNI_DIR}/CelestiaLocation.cpp
        ${CELESTIA_JNI_DIR}/CelestiaStarBrowser.cpp
        ${CELESTIA_JNI_DIR}/CelestiaScript.cpp
        ${CELESTIA_JNI_DIR}/CelestiaUtils.cpp
        ${CELESTIA_JNI_DIR}/CelestiaOrbit.cpp
        ${CELESTIA_JNI_DIR}/CelestiaRotationModel.cpp
        ${CELESTIA_JNI_DIR}/CelestiaUniversalCoord.cpp
        ${CELESTIA_JNI_DIR}/CelestiaDMS.cpp
        ${CELESTIA_JNI_DIR}/CelestiaVector.cpp
        ${CELESTIA_JNI_DIR}/CelestiaEclipseFinder.cpp
        ${CELESTIA_JNI_DIR}/CelestiaPlanetarySystem.cpp
        ${CELESTIA_JNI_DIR}/CelestiaObserver.cpp
        ${CELESTIA_JNI_DIR}/CelestiaRenderer.cpp
        )

set(THIRD_PARTY_DIR "${CMAKE_SOURCE_DIR}/libs")
set(THIRD_PARTY_LIB_DIR "${THIRD_PARTY_DIR}/libs")
set(THIRD_PARTY_INCLUDE_DIR "${THIRD_PARTY_DIR}/include")

set(CMAKE_CXX_STANDARD 11)

add_definitions(-DENABLE_NLS)
add_definitions(-DUSE_SPICE)
add_definitions(-DEIGEN_NO_DEBUG)
add_definitions(-DGL_ES)

include_directories(
        ${CMAKE_SOURCE_DIR}
        ${CELESTIA_SRC_DIR}
        ${THIRD_PARTY_INCLUDE_DIR}
        ${THIRD_PARTY_INCLUDE_DIR}/cspice
        ${THIRD_PARTY_INCLUDE_DIR}/lua
        ${THIRD_PARTY_INCLUDE_DIR}/eigen3
        ${THIRD_PARTY_INCLUDE_DIR}/gettext
        ${THIRD_PARTY_INCLUDE_DIR}/jpeg
        ${THIRD_PARTY_INCLUDE_DIR}/libpng
        ${THIRD_PARTY_INCLUDE_DIR}/libepoxy
        ${THIRD_PARTY_INCLUDE_DIR}/json
        ${THIRD_PARTY_INCLUDE_DIR}/freetype/freetype2
)

add_library(cspice STATIC IMPORTED)
set_target_properties(cspice PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/cspice.a)

add_library(lua STATIC IMPORTED)
set_target_properties(lua PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/liblua.a)

add_library(fmt STATIC IMPORTED)
set_target_properties(fmt PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libfmt.a)

add_library(gettext STATIC IMPORTED)
set_target_properties(gettext PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libintl.a)

add_library(libepoxy STATIC IMPORTED)
set_target_properties(libepoxy PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libGL.a)

add_library(libogg STATIC IMPORTED)
set_target_properties(libogg PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libogg.a)

add_library(theora STATIC IMPORTED)
set_target_properties(theora PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libtheora.a)

add_library(libpng STATIC IMPORTED)
set_target_properties(libpng PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libpng16.a)

add_library(jpeg STATIC IMPORTED)
set_target_properties(jpeg PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libjpeg.a)

add_library(freetype STATIC IMPORTED)
set_target_properties(freetype PROPERTIES IMPORTED_LOCATION ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libfreetype.a)

add_library( # Sets the name of the library.
        celestia

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${CELESTIA_SOURCES} )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        celestia

        # Links the target library to the log library
        # included in the NDK.
        z
        fmt
        libepoxy
        gettext
        jpeg
        freetype
        lua
        libpng
        theora
        libogg
        cspice
        EGL
        android
        ${log-lib} )