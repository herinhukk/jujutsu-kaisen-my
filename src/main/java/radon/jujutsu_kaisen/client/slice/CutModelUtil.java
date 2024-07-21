package radon.jujutsu_kaisen.client.slice;

import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import radon.jujutsu_kaisen.mixin.client.IAgeableListModelAccessor;
import radon.jujutsu_kaisen.mixin.client.IGeoEntityRendererAccessor;
import radon.jujutsu_kaisen.mixin.client.ILivingEntityRendererAccessor;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class CutModelUtil {
    private static RigidBody.VertexData compress(RigidBody.Triangle[] triangles) {
        List<Vec3> vertices = new ArrayList<>(triangles.length * 3);
        int[] indices = new int[triangles.length * 3];
        float[] uv = new float[triangles.length * 6];

        for (int i = 0; i < triangles.length; i++) {
            RigidBody.Triangle triangle = triangles[i];
            double eps = 0.00001D;
            int idx = epsIndexOf(vertices, triangle.p1.pos, eps);

            if (idx != -1) {
                indices[i * 3] = idx;
            } else {
                indices[i * 3] = vertices.size();
                vertices.add(triangle.p1.pos);
            }

            idx = epsIndexOf(vertices, triangle.p2.pos, eps);

            if (idx != -1) {
                indices[i * 3 + 1] = idx;
            } else {
                indices[i * 3 + 1] = vertices.size();
                vertices.add(triangle.p2.pos);
            }

            idx = epsIndexOf(vertices, triangle.p3.pos, eps);

            if (idx != -1) {
                indices[i * 3 + 2] = idx;
            } else {
                indices[i * 3 + 2] = vertices.size();
                vertices.add(triangle.p3.pos);
            }

            uv[i * 6] = triangle.p1.u;
            uv[i * 6 + 1] = triangle.p1.v;
            uv[i * 6 + 2] = triangle.p2.u;
            uv[i * 6 + 3] = triangle.p2.v;
            uv[i * 6 + 4] = triangle.p3.u;
            uv[i * 6 + 5] = triangle.p3.v;
        }
        RigidBody.VertexData data = new RigidBody.VertexData();
        data.positions = vertices.toArray(new Vec3[0]);
        data.indices = indices;
        data.uv = uv;
        return data;
    }

    private static boolean epsilonEquals(Vec3 a, Vec3 b, double eps) {
        double dx = Math.abs(a.x - b.x);
        double dy = Math.abs(a.y - b.y);
        double dz = Math.abs(a.z - b.z);
        return dx < eps && dy < eps && dz < eps;
    }

    private static int epsIndexOf(List<Vec3> l, Vec3 vec, double eps) {
        for (int i = 0; i < l.size(); i++) {
            if (epsilonEquals(vec, l.get(i), eps)) {
                return i;
            }
        }
        return -1;
    }

    private static double rayPlaneIntercept(Vec3 start, Vec3 ray, float[] plane) {
        double num = -(plane[0] * start.x + plane[1] * start.y + plane[2] * start.z + plane[3]);
        double denom = plane[0] * ray.x + plane[1] * ray.y + plane[2] * ray.z;
        return num / denom;
    }

    private static RigidBody.Triangle[] triangulate(Matrix4f matrix4f, ModelPart.Cube cube) {
        RigidBody.Triangle[] triangles = new RigidBody.Triangle[12];

        int i = 0;

        for (ModelPart.Polygon polygon : cube.polygons) {
            Vector3f tmp = new Vector3f();
            Vec3 v0 = new Vec3(matrix4f.transformPosition(tmp.set(polygon.vertices[0].pos).div(16.0F), tmp));
            Vec3 v1 = new Vec3(matrix4f.transformPosition(tmp.set(polygon.vertices[1].pos).div(16.0F), tmp));
            Vec3 v2 = new Vec3(matrix4f.transformPosition(tmp.set(polygon.vertices[2].pos).div(16.0F), tmp));
            Vec3 v3 = new Vec3(matrix4f.transformPosition(tmp.set(polygon.vertices[3].pos).div(16.0F), tmp));
            float[] uv = new float[6];
            uv[0] = polygon.vertices[0].u;
            uv[1] = polygon.vertices[0].v;
            uv[2] = polygon.vertices[1].u;
            uv[3] = polygon.vertices[1].v;
            uv[4] = polygon.vertices[2].u;
            uv[5] = polygon.vertices[2].v;
            triangles[i++] = new RigidBody.Triangle(v0, v1, v2, uv);
            uv = new float[6];
            uv[0] = polygon.vertices[2].u;
            uv[1] = polygon.vertices[2].v;
            uv[2] = polygon.vertices[3].u;
            uv[3] = polygon.vertices[3].v;
            uv[4] = polygon.vertices[0].u;
            uv[5] = polygon.vertices[0].v;
            triangles[i++] = new RigidBody.Triangle(v2, v3, v0, uv);
        }

        return triangles;
    }

    private static RigidBody.Triangle[] triangulate(Matrix4f matrix4f, GeoCube cube) {
        RigidBody.Triangle[] triangles = new RigidBody.Triangle[12];

        int i = 0;

        for (GeoQuad quad : cube.quads()) {
            GeoVertex[] vertices = quad.vertices();
            
            Vector3f tmp = new Vector3f();
            Vec3 v0 = new Vec3(matrix4f.transformPosition(tmp.set(vertices[0].position()), tmp));
            Vec3 v1 = new Vec3(matrix4f.transformPosition(tmp.set(vertices[1].position()), tmp));
            Vec3 v2 = new Vec3(matrix4f.transformPosition(tmp.set(vertices[2].position()), tmp));
            Vec3 v3 = new Vec3(matrix4f.transformPosition(tmp.set(vertices[3].position()), tmp));
            float[] uv = new float[6];
            uv[0] = vertices[0].texU();
            uv[1] = vertices[0].texV();
            uv[2] = vertices[1].texU();
            uv[3] = vertices[1].texV();
            uv[4] = vertices[2].texU();
            uv[5] = vertices[2].texV();
            triangles[i++] = new RigidBody.Triangle(v0, v1, v2, uv);
            uv = new float[6];
            uv[0] = vertices[2].texU();
            uv[1] = vertices[2].texV();
            uv[2] = vertices[3].texU();
            uv[3] = vertices[3].texV();
            uv[4] = vertices[0].texU();
            uv[5] = vertices[0].texV();
            triangles[i++] = new RigidBody.Triangle(v2, v3, v0, uv);
        }

        return triangles;
    }
    
    private static RigidBody.VertexData[] cutAndCapModelBox(Matrix4f matrix4f, ModelPart.Cube cube, float[] plane) {
        return cutAndCapConvex(triangulate(matrix4f, cube), plane);
    }

    private static RigidBody.VertexData[] cutAndCapModelBox(Matrix4f matrix4f, GeoCube cube, float[] plane) {
        return cutAndCapConvex(triangulate(matrix4f, cube), plane);
    }

    private static Matrix3f eulerToMat(float yaw, float pitch, float roll) {
        Matrix3f mY = new Matrix3f();
        mY.rotateY(-yaw);
        Matrix3f mP = new Matrix3f();
        mP.rotateX(pitch);
        Matrix3f mR = new Matrix3f();
        mR.rotateZ(roll);
        mR.mul(mP);
        mR.mul(mY);
        return mR;
    }

    private static Vec3 getEulerAngles(Vec3 vec) {
        double yaw = Math.toDegrees(Math.atan2(vec.x, vec.z));
        double sqrt = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        double pitch = Math.toDegrees(Math.atan2(vec.y, sqrt));
        return new Vec3(yaw, pitch - 90.0F, 0);
    }

    private static Matrix3f normalToMatrix(Vec3 normal, float roll) {
        Vec3 euler = getEulerAngles(normal);
        return eulerToMat((float) Math.toRadians(euler.x), (float) Math.toRadians(euler.y + 90.0F), roll);
    }

    private static Vec3 getNext(List<Vec3[]> edges, Vec3 first) {
        Iterator<Vec3[]> iter = edges.iterator();

        while (iter.hasNext()) {
            Vec3[] v = iter.next();
            double eps = 0.00001D;

            if (epsilonEquals(v[0], first, eps)) {
                iter.remove();
                return v[1];
            } else if (epsilonEquals(v[1], first, eps)) {
                iter.remove();
                return v[0];
            }
        }
        throw new RuntimeException("Didn't find next in loop!");
    }

    private static RigidBody.VertexData[] cutAndCapConvex(RigidBody.Triangle[] triangles, float[] plane) {
        RigidBody.VertexData[] result = new RigidBody.VertexData[]{null, null, new RigidBody.VertexData()};
        List<RigidBody.Triangle> side1 = new ArrayList<>();
        List<RigidBody.Triangle> side2 = new ArrayList<>();
        List<Vec3[]> clippedEdges = new ArrayList<>();

        for (RigidBody.Triangle triangle : triangles) {
            boolean p1 = triangle.p1.pos.x * plane[0] + triangle.p1.pos.y * plane[1] + triangle.p1.pos.z * plane[2] + plane[3] > 0;
            boolean p2 = triangle.p2.pos.x * plane[0] + triangle.p2.pos.y * plane[1] + triangle.p2.pos.z * plane[2] + plane[3] > 0;
            boolean p3 = triangle.p3.pos.x * plane[0] + triangle.p3.pos.y * plane[1] + triangle.p3.pos.z * plane[2] + plane[3] > 0;

            if (p1 && p2 && p3) { // If all points on positive side, add to side 1
                side1.add(triangle);
            } else if (!p1 && !p2 && !p3) { // Else if all on negative side, add to size 2
                side2.add(triangle);
            } else if (p1 ^ p2 ^ p3) { // Else if only one is positive, clip and add 1 triangle to side 1, 2 to side 2
                RigidBody.Triangle.TexVertex a, b, c;

                if (p1) {
                    a = triangle.p1;
                    b = triangle.p2;
                    c = triangle.p3;
                } else if (p2) {
                    a = triangle.p2;
                    b = triangle.p3;
                    c = triangle.p1;
                } else {
                    a = triangle.p3;
                    b = triangle.p1;
                    c = triangle.p2;
                }
                Vec3 rAB = b.pos.subtract(a.pos);
                Vec3 rAC = c.pos.subtract(a.pos);
                float interceptAB = (float) rayPlaneIntercept(a.pos, rAB, plane);
                float interceptAC = (float) rayPlaneIntercept(a.pos, rAC, plane);
                Vec3 d = a.pos.add(rAB.scale(interceptAB));
                Vec3 e = a.pos.add(rAC.scale(interceptAC));
                float[] deTex = new float[4];
                deTex[0] = a.u + (b.u - a.u) * interceptAB;
                deTex[1] = a.v + (b.v - a.v) * interceptAB;
                deTex[2] = a.u + (c.u - a.u) * interceptAC;
                deTex[3] = a.v + (c.v - a.v) * interceptAC;

                side2.add(new RigidBody.Triangle(d, b.pos, e, new float[]{deTex[0], deTex[1], b.u, b.v, deTex[2], deTex[3]}));
                side2.add(new RigidBody.Triangle(b.pos, c.pos, e, new float[]{b.u, b.v, c.u, c.v, deTex[2], deTex[3]}));
                side1.add(new RigidBody.Triangle(a.pos, d, e, new float[]{a.u, a.v, deTex[0], deTex[1], deTex[2], deTex[3]}));
                clippedEdges.add(new Vec3[]{d, e});
            } else { // Else one is negative, clip and add 2 triangles to side 1, 1 to side 2.
                RigidBody.Triangle.TexVertex a, b, c;

                if (!p1) {
                    a = triangle.p1;
                    b = triangle.p2;
                    c = triangle.p3;
                } else if (!p2) {
                    a = triangle.p2;
                    b = triangle.p3;
                    c = triangle.p1;
                } else {
                    a = triangle.p3;
                    b = triangle.p1;
                    c = triangle.p2;
                }
                Vec3 rAB = b.pos.subtract(a.pos);
                Vec3 rAC = c.pos.subtract(a.pos);
                float interceptAB = (float) rayPlaneIntercept(a.pos, rAB, plane);
                float interceptAC = (float) rayPlaneIntercept(a.pos, rAC, plane);
                Vec3 d = a.pos.add(rAB.scale(interceptAB));
                Vec3 e = a.pos.add(rAC.scale(interceptAC));
                float[] deTex = new float[4];
                deTex[0] = a.u + (b.u - a.u) * interceptAB;
                deTex[1] = a.v + (b.v - a.v) * interceptAB;
                deTex[2] = a.u + (c.u - a.u) * interceptAC;
                deTex[3] = a.v + (c.v - a.v) * interceptAC;

                side1.add(new RigidBody.Triangle(d, b.pos, e, new float[] { deTex[0], deTex[1], b.u, b.v, deTex[2], deTex[3] }));
                side1.add(new RigidBody.Triangle(b.pos, c.pos, e, new float[] { b.u, b.v, c.u, c.v, deTex[2], deTex[3] }));
                side2.add(new RigidBody.Triangle(a.pos, d, e, new float[] { a.u, a.v, deTex[0], deTex[1], deTex[2], deTex[3] }));

                clippedEdges.add(new Vec3[] { e, d });
            }
        }

        if (!clippedEdges.isEmpty()) {
            Matrix3f matrix3f = normalToMatrix(new Vec3(plane[0], plane[1], plane[2]), 0.0F);
            List<Vec3> orderedClipVertices = new ArrayList<>();
            orderedClipVertices.add(clippedEdges.getFirst()[0]);

            while (!clippedEdges.isEmpty()) {
                orderedClipVertices.add(getNext(clippedEdges, orderedClipVertices.getLast()));
            }

            Vector3f uv1 = new Vector3f((float) orderedClipVertices.getFirst().x, (float) orderedClipVertices.getFirst().y, (float) orderedClipVertices.getFirst().z);
            matrix3f.transform(uv1);
            RigidBody.Triangle[] cap = new RigidBody.Triangle[orderedClipVertices.size() - 2];

            for (int i = 0; i < cap.length; i++) {
                Vector3f uv2 = new Vector3f((float) orderedClipVertices.get(i + 2).x, (float) orderedClipVertices.get(i + 2).y, (float) orderedClipVertices.get(i + 2).z);
                matrix3f.transform(uv2);
                Vector3f uv3 = new Vector3f((float) orderedClipVertices.get(i + 1).x, (float) orderedClipVertices.get(i + 1).y, (float) orderedClipVertices.get(i + 1).z);
                matrix3f.transform(uv3);
                cap[i] = new RigidBody.Triangle(orderedClipVertices.getFirst(), orderedClipVertices.get(i + 2), orderedClipVertices.get(i + 1),
                        new float[] { uv1.x, uv1.y, uv2.x, uv2.y, uv3.x, uv3.y });
                side1.add(new RigidBody.Triangle(orderedClipVertices.getFirst(), orderedClipVertices.get(i + 2), orderedClipVertices.get(i + 1), new float[6]));
                side2.add(new RigidBody.Triangle(orderedClipVertices.getFirst(), orderedClipVertices.get(i + 1), orderedClipVertices.get(i + 2), new float[6]));
            }
            result[2] = compress(cap);
        }
        result[0] = compress(side1.toArray(new RigidBody.Triangle[0]));
        result[1] = compress(side2.toArray(new RigidBody.Triangle[0]));
        return result;
    }

    private static void collect(PoseStack poseStack, HierarchicalModel<?> hierarchical, List<Pair<ModelPart, Matrix4f>> boxes) {
        hierarchical.root().getAllParts().forEach(part ->
                boxes.add(Pair.of(part, poseStack.last().pose())));
    }

    private static void collect(PoseStack poseStack, AgeableListModel<?> ageable, List<Pair<ModelPart, Matrix4f>> boxes) {
        if (ageable.young) {
            poseStack.pushPose();

            if (ageable.scaleHead) {
                float f10 = 1.5F / ageable.babyHeadScale;
                poseStack.scale(f10, f10, f10);
            }

            poseStack.translate(0.0F, ageable.babyYHeadOffset / 16.0F, ageable.babyZHeadOffset / 16.0F);
            ((IAgeableListModelAccessor) ageable).invokeHeadParts()
                    .forEach(part -> collect(poseStack, part, boxes));
            poseStack.popPose();

            poseStack.pushPose();
            float f11 = 1.0F / ageable.babyBodyScale;
            poseStack.scale(f11, f11, f11);
            poseStack.translate(0.0F, ageable.bodyYOffset / 16.0F, 0.0F);
            ((IAgeableListModelAccessor) ageable).invokeBodyParts()
                    .forEach(part -> collect(poseStack, part, boxes));
            poseStack.popPose();
        } else {
            ((IAgeableListModelAccessor) ageable).invokeHeadParts()
                    .forEach(part -> collect(poseStack, part, boxes));
            ((IAgeableListModelAccessor) ageable).invokeBodyParts()
                    .forEach(part -> collect(poseStack, part, boxes));
        }
    }

    private static void collect(PoseStack poseStack, ModelPart part, List<Pair<ModelPart, Matrix4f>> boxes) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        boxes.add(Pair.of(part, poseStack.last().pose()));

        for (ModelPart child : part.children.values()) {
            collect(poseStack, child, boxes);
        }
        poseStack.popPose();
    }

    private static void collect(PoseStack poseStack, GeoBone bone, List<Pair<GeoBone, Matrix4f>> boxes) {
        poseStack.pushPose();
        RenderUtil.prepMatrixForBone(poseStack, bone);
        boxes.add(Pair.of(bone, poseStack.last().pose()));

        for (GeoBone child : bone.getChildBones()) {
            collect(poseStack, child, boxes);
        }
        poseStack.popPose();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collect(LivingEntity entity, LivingEntityRenderer renderer, float partialTicks, List<Pair<ModelPart, Matrix4f>> boxes) {
        EntityModel model = renderer.getModel();

        PoseStack poseStack = new PoseStack();

        model.attackTime = ((ILivingEntityRendererAccessor) renderer).invokeGetAttackAnim(entity, partialTicks);
        boolean shouldSit = entity.isPassenger() && (entity.getVehicle() != null && entity.getVehicle().shouldRiderSit());
        model.riding = shouldSit;
        model.young = entity.isBaby();
        float f = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float f1 = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
        float f2 = f1 - f;

        if (shouldSit && entity.getVehicle() instanceof LivingEntity livingentity) {
            f = Mth.rotLerp(partialTicks, livingentity.yBodyRotO, livingentity.yBodyRot);
            f2 = f1 - f;
            float f7 = Mth.wrapDegrees(f2);

            if (f7 < -85.0F) {
                f7 = -85.0F;
            }

            if (f7 >= 85.0F) {
                f7 = 85.0F;
            }

            f = f1 - f7;

            if (f7 * f7 > 2500.0F) {
                f += f7 * 0.2F;
            }

            f2 = f1 - f;
        }

        float f6 = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        if (LivingEntityRenderer.isEntityUpsideDown(entity)) {
            f6 *= -1.0F;
            f2 *= -1.0F;
        }

        f2 = Mth.wrapDegrees(f2);

        if (entity.hasPose(Pose.SLEEPING)) {
            Direction direction = entity.getBedOrientation();
            if (direction != null) {
                float f3 = entity.getEyeHeight(Pose.STANDING) - 0.1F;
                poseStack.translate((float) (-direction.getStepX()) * f3, 0.0F, (float) (-direction.getStepZ()) * f3);
            }
        }

        float f8 = entity.getScale();
        poseStack.scale(f8, f8, f8);
        float f9 = ((ILivingEntityRendererAccessor) renderer).invokeGetBob(entity, partialTicks);
        ((ILivingEntityRendererAccessor) renderer).invokeSetupRotations(entity, poseStack, f9, f, partialTicks, f8);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        ((ILivingEntityRendererAccessor) renderer).invokeScale(entity, poseStack, partialTicks);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        model.prepareMobModel(entity, 0.0F, 0.0F, partialTicks);
        model.setupAnim(entity, 0.0F, 0.0F, f9, f2, f6);

        if (model instanceof HierarchicalModel<?> hierarchical) {
            collect(poseStack, hierarchical, boxes);
        } else if (model instanceof AgeableListModel<?> ageable) {
            collect(poseStack, ageable, boxes);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collect(LivingEntity entity, GeoEntityRenderer renderer, float partialTicks, List<Pair<GeoBone, Matrix4f>> boxes) {
        PoseStack poseStack = new PoseStack();

        boolean shouldSit = entity.isPassenger() && (entity.getVehicle() != null);
        float lerpBodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float lerpHeadRot = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);

        if (shouldSit && entity.getVehicle() instanceof LivingEntity vehicle) {
            lerpBodyRot = Mth.rotLerp(partialTicks, vehicle.yBodyRotO, vehicle.yBodyRot);
            float netHeadYaw = lerpHeadRot - lerpBodyRot;
            float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85.0F, 85.0F);
            lerpBodyRot = lerpHeadRot - clampedHeadYaw;

            if (clampedHeadYaw * clampedHeadYaw > 2500.0F) lerpBodyRot += clampedHeadYaw * 0.2F;
        }

        if (entity.getPose() == Pose.SLEEPING) {
            Direction bedDirection = entity.getBedOrientation();

            if (bedDirection != null) {
                float eyePosOffset = entity.getEyeHeight(Pose.STANDING) - 0.1F;
                poseStack.translate(-bedDirection.getStepX() * eyePosOffset, 0.0D, -bedDirection.getStepZ() * eyePosOffset);
            }
        }

        float ageInTicks = entity.tickCount + partialTicks;
        ((IGeoEntityRendererAccessor) renderer).invokeApplyRotations(entity, poseStack, ageInTicks, lerpBodyRot, partialTicks);

        poseStack.translate(0, 0.01F, 0);

        GeoModel<?> model = renderer.getGeoModel();
        model.getAnimationProcessor().getRegisteredBones().forEach(bone -> {
            if (bone.getParent() == null) collect(poseStack, bone, boxes);
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void collect(LivingEntity entity, LivingEntityRenderer renderer, Vector3f plane, float distance, float partialTicks, List<RigidBody.CutModelData> top, List<RigidBody.CutModelData> bottom) {
        List<Pair<ModelPart, Matrix4f>> boxes = new ArrayList<>();
        collect(entity, renderer, partialTicks, boxes);

        Minecraft mc = Minecraft.getInstance();
        ResourceLocation texture = renderer.getTextureLocation(entity);
        ResourceManager manager = mc.getResourceManager();
        Optional<Resource> resource = manager.getResource(texture);

        if (resource.isEmpty()) return;

        NativeImage image;

        try {
            image = NativeImage.read(resource.get().open());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Pair<ModelPart, Matrix4f> pair : boxes) {
            if (!pair.first.visible || pair.first.skipDraw) continue;

            boolean visible = false;

            // Go through every cube in the model and check if it has any non-transparent pixels
            for (ModelPart.Cube cube : pair.first.cubes) {
                if (visible) break;

                for (ModelPart.Polygon polygon : cube.polygons) {
                    if (visible) break;

                    float[] uv = new float[8];
                    uv[0] = polygon.vertices[0].u;
                    uv[1] = polygon.vertices[0].v;
                    uv[2] = polygon.vertices[1].u;
                    uv[3] = polygon.vertices[1].v;
                    uv[4] = polygon.vertices[2].u;
                    uv[5] = polygon.vertices[2].v;
                    uv[6] = polygon.vertices[3].u;
                    uv[7] = polygon.vertices[3].v;

                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();

                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

                    for (int i = 0; i < uv.length; i += 2) {
                        float u = uv[i];
                        float v = uv[i + 1];

                        if (u < minX) minX = u;
                        if (u > maxX) maxX = u;
                        if (v < minY) minY = v;
                        if (v > maxY) maxY = v;
                    }

                    int startX = Math.max(0, Math.round(minX * (imageWidth - 1)));
                    int endX = Math.min(imageWidth - 1, Math.round(maxX * (imageWidth - 1)));
                    int startY = Math.max(0, Math.round(minY * (imageHeight - 1)));
                    int endY = Math.min(imageHeight - 1, Math.round(maxY * (imageHeight - 1)));

                    for (int x = startX; x < endX; x++) {
                        if (visible) break;

                        for (int y = startY; y < endY; y++) {
                            int color = image.getPixelRGBA(x, y);

                            if (FastColor.ARGB32.alpha(color) == 0) continue;

                            visible = true;
                            break;
                        }
                    }
                }
            }

            if (!visible) continue;

            for (ModelPart.Cube cube : pair.first.cubes) {
                RigidBody.VertexData[] data = cutAndCapModelBox(pair.second, cube, new float[] { plane.x, plane.y, plane.z, -distance });
                RigidBody.CutModelData tp = null;
                RigidBody.CutModelData bt = null;

                if (data[0].indices != null && data[0].indices.length > 0) {
                    tp = new RigidBody.CutModelData(data[0], null, false,
                            new ConvexMeshCollider(data[0].indices, data[0].vertices(), 1.0F));
                    top.add(tp);
                }
                if (data[1].indices != null && data[1].indices.length > 0) {
                    bt = new RigidBody.CutModelData(data[1], null, true,
                            new ConvexMeshCollider(data[1].indices, data[1].vertices(), 1.0F));
                    bottom.add(bt);
                }
                if (data[2].indices != null && data[2].indices.length > 0) {
                    tp.cap = data[2];
                    bt.cap = data[2];
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void collect(LivingEntity entity, GeoEntityRenderer renderer, Vector3f plane, float distance, float partialTicks, List<RigidBody.CutModelData> top, List<RigidBody.CutModelData> bottom) {
        List<Pair<GeoBone, Matrix4f>> boxes = new ArrayList<>();
        collect(entity, renderer, partialTicks, boxes);

        Minecraft mc = Minecraft.getInstance();
        ResourceLocation texture = renderer.getTextureLocation(entity);
        ResourceManager manager = mc.getResourceManager();
        Optional<Resource> resource = manager.getResource(texture);

        if (resource.isEmpty()) return;

        NativeImage image;

        try {
            image = NativeImage.read(resource.get().open());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Pair<GeoBone, Matrix4f> pair : boxes) {
            if (pair.first.isHidden() || pair.first.shouldNeverRender() == Boolean.TRUE) continue;

            boolean visible = false;

            // Go through every cube in the model and check if it has any non-transparent pixels
            for (GeoCube cube : pair.first.getCubes()) {
                if (visible) break;

                for (GeoQuad quad : cube.quads()) {
                    if (visible) break;

                    GeoVertex[] vertices = quad.vertices();
                    
                    float[] uv = new float[8];
                    uv[0] = vertices[0].texU();
                    uv[1] = vertices[0].texV();
                    uv[2] = vertices[1].texU();
                    uv[3] = vertices[1].texV();
                    uv[4] = vertices[2].texU();
                    uv[5] = vertices[2].texV();
                    uv[6] = vertices[3].texU();
                    uv[7] = vertices[3].texV();

                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();

                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

                    for (int i = 0; i < uv.length; i += 2) {
                        float u = uv[i];
                        float v = uv[i + 1];

                        if (u < minX) minX = u;
                        if (u > maxX) maxX = u;
                        if (v < minY) minY = v;
                        if (v > maxY) maxY = v;
                    }

                    int startX = Math.max(0, Math.round(minX * (imageWidth - 1)));
                    int endX = Math.min(imageWidth - 1, Math.round(maxX * (imageWidth - 1)));
                    int startY = Math.max(0, Math.round(minY * (imageHeight - 1)));
                    int endY = Math.min(imageHeight - 1, Math.round(maxY * (imageHeight - 1)));

                    for (int x = startX; x < endX; x++) {
                        if (visible) break;

                        for (int y = startY; y < endY; y++) {
                            int color = image.getPixelRGBA(x, y);

                            if (FastColor.ARGB32.alpha(color) == 0) continue;

                            visible = true;
                            break;
                        }
                    }
                }
            }

            if (!visible) continue;

            for (GeoCube cube : pair.first.getCubes()) {
                RigidBody.VertexData[] data = cutAndCapModelBox(pair.second, cube, new float[] { plane.x, plane.y, plane.z, -distance });
                RigidBody.CutModelData tp = null;
                RigidBody.CutModelData bt = null;

                if (data[0].indices != null && data[0].indices.length > 0) {
                    tp = new RigidBody.CutModelData(data[0], null, false,
                            new ConvexMeshCollider(data[0].indices, data[0].vertices(), 1.0F));
                    top.add(tp);
                }
                if (data[1].indices != null && data[1].indices.length > 0) {
                    bt = new RigidBody.CutModelData(data[1], null, true,
                            new ConvexMeshCollider(data[1].indices, data[1].vertices(), 1.0F));
                    bottom.add(bt);
                }
                if (data[2].indices != null && data[2].indices.length > 0) {
                    tp.cap = data[2];
                    bt.cap = data[2];
                }
            }
        }
    }
    
    @SuppressWarnings("rawtypes")
    public static void collect(LivingEntity entity, Vector3f plane, float distance, float partialTicks, List<RigidBody.CutModelData> top, List<RigidBody.CutModelData> bottom) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        if (dispatcher.getRenderer(entity) instanceof LivingEntityRenderer living) {
            collect(entity, living, plane, distance, partialTicks, top, bottom);
        } else if (dispatcher.getRenderer(entity) instanceof GeoEntityRenderer geo) {
            collect(entity, geo, plane, distance, partialTicks, top, bottom);
        }
    }
}