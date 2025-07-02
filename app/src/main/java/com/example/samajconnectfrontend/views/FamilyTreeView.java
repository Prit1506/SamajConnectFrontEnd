package com.example.samajconnectfrontend.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.samajconnectfrontend.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class FamilyTreeView extends View {
    private static final String TAG = "FamilyTreeView";

    // Drawing constants
    private static final int NODE_RADIUS = 120;
    private static final int NODE_SPACING_X = 400;
    private static final int MIN_NODE_SPACING_X = 600;
    private static final int NODE_SPACING_Y = 450;
    private static final int LINE_WIDTH = 4;
    private static final int TEXT_SIZE = 28;
    private static final int RELATIONSHIP_TEXT_SIZE = 22;
    private static final int LEVEL_TEXT_SIZE = 20;
    private static final int NAME_MARGIN = 80;
    private static final int RELATIONSHIP_MARGIN = 120;
    private static final int PROFILE_MARGIN = 25;
    private static final int TEXT_PADDING = 20;
    private static final int MIN_TEXT_SPACING = 60;
    private static final int MAX_TEXT_WIDTH = 300;
    private static final int TEXT_LINE_SPACING = 8;

    // Enhanced colors
    private static final int NODE_COLOR = Color.parseColor("#FFFFFF");
    private static final int NODE_BORDER_COLOR = Color.parseColor("#2196F3");
    private static final int LINE_COLOR = Color.parseColor("#4CAF50");
    private static final int TEXT_COLOR = Color.parseColor("#000000");
    private static final int ROOT_NODE_COLOR = Color.parseColor("#E8F5E8");
    private static final int ROOT_NODE_BORDER_COLOR = Color.parseColor("#4CAF50");
    private static final int RELATIONSHIP_TEXT_COLOR = Color.parseColor("#333333");
    private static final int LEVEL_TEXT_COLOR = Color.parseColor("#666666");

    // Paint objects
    private Paint nodePaint;
    private Paint nodeBorderPaint;
    private Paint linePaint;
    private Paint textPaint;
    private Paint relationshipTextPaint;
    private Paint rootNodePaint;
    private Paint rootNodeBorderPaint;
    private Paint levelPaint;
    private Paint textBackgroundPaint;

    // Data structures
    private List<FamilyNode> familyNodes;
    private Map<Integer, List<FamilyNode>> generationMap;
    private FamilyNode rootNode;

    // NEW: Store actual relationships from database
    private List<DatabaseRelationship> databaseRelationships;

    // Touch and zoom handling
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 0.8f;
    private float translateX = 0f;
    private float translateY = 0f;

    // Callbacks
    private OnNodeClickListener nodeClickListener;
    private OnNodeLongClickListener nodeLongClickListener;

    public interface OnNodeClickListener {
        void onNodeClick(FamilyNode node);
    }

    public interface OnNodeLongClickListener {
        void onNodeLongClick(FamilyNode node);
    }

    public FamilyTreeView(Context context) {
        super(context);
        init();
    }

    public FamilyTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        familyNodes = new ArrayList<>();
        generationMap = new HashMap<>();
        databaseRelationships = new ArrayList<>();

        // Initialize paints
        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setColor(NODE_COLOR);
        nodePaint.setStyle(Paint.Style.FILL);
        nodePaint.setShadowLayer(10, 0, 6, Color.parseColor("#40000000"));

        nodeBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodeBorderPaint.setColor(NODE_BORDER_COLOR);
        nodeBorderPaint.setStyle(Paint.Style.STROKE);
        nodeBorderPaint.setStrokeWidth(8);

        rootNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rootNodePaint.setColor(ROOT_NODE_COLOR);
        rootNodePaint.setStyle(Paint.Style.FILL);
        rootNodePaint.setShadowLayer(12, 0, 8, Color.parseColor("#40000000"));

        rootNodeBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rootNodeBorderPaint.setColor(ROOT_NODE_BORDER_COLOR);
        rootNodeBorderPaint.setStyle(Paint.Style.STROKE);
        rootNodeBorderPaint.setStrokeWidth(10);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setSubpixelText(true);
        textPaint.setLinearText(true);
        textPaint.setShadowLayer(2, 1, 1, Color.parseColor("#80FFFFFF"));

        relationshipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        relationshipTextPaint.setColor(RELATIONSHIP_TEXT_COLOR);
        relationshipTextPaint.setTextSize(RELATIONSHIP_TEXT_SIZE);
        relationshipTextPaint.setTextAlign(Paint.Align.CENTER);
        relationshipTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        relationshipTextPaint.setSubpixelText(true);
        relationshipTextPaint.setLinearText(true);
        relationshipTextPaint.setShadowLayer(2, 1, 1, Color.parseColor("#80FFFFFF"));

        levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelPaint.setColor(LEVEL_TEXT_COLOR);
        levelPaint.setTextSize(LEVEL_TEXT_SIZE);
        levelPaint.setTextAlign(Paint.Align.LEFT);
        levelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        textBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textBackgroundPaint.setColor(Color.parseColor("#FFFFFF"));
        textBackgroundPaint.setAlpha(240);
        textBackgroundPaint.setShadowLayer(6, 0, 3, Color.parseColor("#30000000"));

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(LINE_COLOR);
        linePaint.setStrokeWidth(LINE_WIDTH);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        // Initialize gesture detectors
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void setOnNodeClickListener(OnNodeClickListener listener) {
        this.nodeClickListener = listener;
    }

    public void setOnNodeLongClickListener(OnNodeLongClickListener listener) {
        this.nodeLongClickListener = listener;
    }

    public void loadFamilyTreeData(JSONObject treeData) {
        try {
            Log.d(TAG, "=== LOADING FAMILY TREE DATA ===");
            Log.d(TAG, "Raw tree data: " + treeData.toString());

            familyNodes.clear();
            generationMap.clear();
            databaseRelationships.clear();

            // Parse root user
            if (treeData.has("rootUser")) {
                JSONObject rootUserData = treeData.getJSONObject("rootUser");
                Log.d(TAG, "Root user data: " + rootUserData.toString());
                rootNode = createNodeFromJson(rootUserData, true);
                familyNodes.add(rootNode);
                Log.d(TAG, "Root node created: " + rootNode.name + " (ID: " + rootNode.userId + ")");
            } else {
                Log.e(TAG, "No rootUser found in tree data!");
            }

            // Parse generations and extract relationships
            if (treeData.has("generations")) {
                JSONArray generations = treeData.getJSONArray("generations");
                Log.d(TAG, "Found " + generations.length() + " generations");

                for (int i = 0; i < generations.length(); i++) {
                    JSONObject generation = generations.getJSONObject(i);
                    int level = generation.getInt("level");
                    JSONArray allMembers = generation.getJSONArray("allMembers");
                    List<FamilyNode> levelNodes = new ArrayList<>();

                    Log.d(TAG, "Generation " + level + " has " + allMembers.length() + " members");

                    for (int j = 0; j < allMembers.length(); j++) {
                        JSONObject member = allMembers.getJSONObject(j);
                        Log.d(TAG, "Processing member: " + member.toString());
                        FamilyNode node = createNodeFromJson(member, false);
                        familyNodes.add(node);
                        levelNodes.add(node);

                        // IMPORTANT: Store the actual database relationship
                        DatabaseRelationship dbRel = new DatabaseRelationship();
                        dbRel.fromUserId = rootNode.userId; // Root user
                        dbRel.toUserId = node.userId; // Related user
                        dbRel.relationshipType = node.relationshipDisplayName;
                        dbRel.generationLevel = node.generationLevel;
                        databaseRelationships.add(dbRel);

                        Log.d(TAG, "Added relationship: " + rootNode.userId + " -> " + node.userId + " (" + node.relationshipDisplayName + ")");
                        Log.d(TAG, "Added node: " + node.name + " (ID: " + node.userId + ") (" + node.relationshipDisplayName + ") at level " + level + " side: " + node.relationshipSide);
                    }
                    generationMap.put(level, levelNodes);
                }
            } else {
                Log.e(TAG, "No generations found in tree data!");
            }

            Log.d(TAG, "Total nodes created: " + familyNodes.size());
            Log.d(TAG, "Total database relationships: " + databaseRelationships.size());
            Log.d(TAG, "Root node: " + (rootNode != null ? rootNode.name + " (ID: " + rootNode.userId + ")" : "NULL"));

            // Log all database relationships
            logDatabaseRelationships();

            // Calculate positions
            calculateOptimizedNodePositions();

            // Center the view on root node
            centerOnRoot();
            invalidate();

            Log.d(TAG, "=== FAMILY TREE DATA LOADED ===");
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing family tree data", e);
        }
    }

    private void logDatabaseRelationships() {
        Log.d(TAG, "=== DATABASE RELATIONSHIPS ===");
        for (DatabaseRelationship rel : databaseRelationships) {
            Log.d(TAG, "Relationship: " + rel.fromUserId + " -> " + rel.toUserId + " (" + rel.relationshipType + ") Level: " + rel.generationLevel);
        }
        Log.d(TAG, "===============================");
    }

    private FamilyNode createNodeFromJson(JSONObject jsonData, boolean isRoot) throws JSONException {
        FamilyNode node = new FamilyNode();
        node.userId = jsonData.getLong("userId");
        node.name = jsonData.getString("name");
        node.email = jsonData.optString("email", "");
        node.isRoot = isRoot;

        Log.d(TAG, "Creating node: " + node.name + " (ID: " + node.userId + ") isRoot: " + isRoot);

        if (!isRoot) {
            node.relationshipDisplayName = jsonData.optString("relationshipDisplayName", "");
            node.generationLevel = jsonData.optInt("generationLevel", 0);
            node.generationName = jsonData.optString("generationName", "");
            node.relationshipSide = jsonData.optString("relationshipSide", "");
            Log.d(TAG, " - Relationship: " + node.relationshipDisplayName);
            Log.d(TAG, " - Generation Level: " + node.generationLevel);
            Log.d(TAG, " - Generation Name: " + node.generationName);
            Log.d(TAG, " - Relationship Side: " + node.relationshipSide);
        } else {
            node.generationLevel = 0;
            Log.d(TAG, " - This is the ROOT user");
        }

        // Handle profile image
        if (jsonData.has("profileImageBase64") && !jsonData.isNull("profileImageBase64")) {
            String base64Image = jsonData.getString("profileImageBase64");
            if (!base64Image.isEmpty()) {
                try {
                    byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                    Bitmap originalBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (originalBitmap != null) {
                        node.profileBitmap = createHighQualityCircularBitmap(originalBitmap);
                        Log.d(TAG, " - Profile image loaded successfully");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding profile image for " + node.name, e);
                }
            }
        }

        return node;
    }

    private Bitmap createHighQualityCircularBitmap(Bitmap bitmap) {
        int desiredSize = (NODE_RADIUS - PROFILE_MARGIN) * 2;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, desiredSize, desiredSize, true);
        Bitmap output = Bitmap.createBitmap(desiredSize, desiredSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawCircle(desiredSize / 2f, desiredSize / 2f, desiredSize / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaledBitmap, 0, 0, paint);
        return output;
    }

    private void calculateOptimizedNodePositions() {
        if (rootNode == null) return;

        int centerX = 2000;
        int centerY = 2000;

        // Position root node at center
        rootNode.x = centerX;
        rootNode.y = centerY;
        Log.d(TAG, "Root positioned at: (" + rootNode.x + ", " + rootNode.y + ")");

        // Sort generations by level
        List<Integer> sortedLevels = new ArrayList<>(generationMap.keySet());
        Collections.sort(sortedLevels);
        Log.d(TAG, "Generation levels found: " + sortedLevels.toString());

        // Position each generation
        for (int level : sortedLevels) {
            List<FamilyNode> levelNodes = generationMap.get(level);
            if (levelNodes == null || levelNodes.isEmpty()) continue;

            Log.d(TAG, "Positioning generation " + level + " with " + levelNodes.size() + " nodes");

            int yPosition;
            if (level == 0) {
                yPosition = centerY;
                Log.d(TAG, "Level 0 (same generation) - positioning at same Y as root: " + yPosition);
            } else {
                yPosition = centerY + (level * NODE_SPACING_Y);
                Log.d(TAG, "Level " + level + " - positioning at Y: " + yPosition);
            }

            positionNodesWithoutOverlap(levelNodes, centerX, yPosition, level);
        }

        logNodePositions();
    }

    private void positionNodesWithoutOverlap(List<FamilyNode> nodes, int centerX, int yPosition, int level) {
        if (nodes.isEmpty()) return;

        Map<String, List<FamilyNode>> sideGroups = groupNodesBySide(nodes);
        int requiredSpacing = calculateMinimumSpacing(nodes);
        int totalNodes = nodes.size();

        if (totalNodes == 1) {
            FamilyNode singleNode = nodes.get(0);
            if (level == 0) {
                singleNode.x = centerX + (NODE_SPACING_X);
                Log.d(TAG, "Single level 0 node " + singleNode.name + " positioned to the right of root");
            } else {
                singleNode.x = centerX;
            }
            singleNode.y = yPosition;
            Log.d(TAG, "Single node " + singleNode.name + " positioned at: (" + singleNode.x + ", " + singleNode.y + ")");
            return;
        }

        int totalWidth = (totalNodes - 1) * requiredSpacing;
        int startX;
        if (level == 0) {
            startX = centerX + (NODE_SPACING_X / 2) - totalWidth / 2;
            Log.d(TAG, "Level 0 (same generation) - starting X position: " + startX);
        } else {
            startX = centerX - totalWidth / 2;
            Log.d(TAG, "Level " + level + " - starting X position: " + startX);
        }

        int currentIndex = 0;
        String[] sideOrder = {"PATERNAL", "DIRECT", "MATERNAL", "SPOUSE_FAMILY", "STEP_FAMILY"};
        for (String side : sideOrder) {
            List<FamilyNode> sideNodes = sideGroups.get(side);
            if (sideNodes == null || sideNodes.isEmpty()) continue;

            for (FamilyNode node : sideNodes) {
                node.x = startX + (currentIndex * requiredSpacing);
                node.y = yPosition;
                Log.d(TAG, "Node " + node.name + " (" + node.relationshipDisplayName + ") positioned at: (" + node.x + ", " + node.y + ") - Side: " + side + " - Level: " + level);
                currentIndex++;
            }
        }
    }

    private int calculateMinimumSpacing(List<FamilyNode> nodes) {
        int maxTextWidth = 0;
        for (FamilyNode node : nodes) {
            String fullName = node.name != null ? node.name : "";
            List<String> nameLines = wrapText(fullName, textPaint, MAX_TEXT_WIDTH);
            int maxLineWidth = 0;
            for (String line : nameLines) {
                int lineWidth = (int) textPaint.measureText(line);
                maxLineWidth = Math.max(maxLineWidth, lineWidth);
            }

            int relationshipWidth = 0;
            if (!node.isRoot && node.relationshipDisplayName != null && !node.relationshipDisplayName.isEmpty()) {
                List<String> relationshipLines = wrapText(node.relationshipDisplayName, relationshipTextPaint, MAX_TEXT_WIDTH);
                for (String line : relationshipLines) {
                    int lineWidth = (int) relationshipTextPaint.measureText(line);
                    relationshipWidth = Math.max(relationshipWidth, lineWidth);
                }
            }

            int totalTextWidth = Math.max(maxLineWidth, relationshipWidth) + (TEXT_PADDING * 2);
            maxTextWidth = Math.max(maxTextWidth, totalTextWidth);
        }

        int minimumSpacing = (NODE_RADIUS * 3) + maxTextWidth + MIN_TEXT_SPACING;
        return Math.max(MIN_NODE_SPACING_X, minimumSpacing);
    }

    private Map<String, List<FamilyNode>> groupNodesBySide(List<FamilyNode> nodes) {
        Map<String, List<FamilyNode>> groups = new LinkedHashMap<>();
        String[] sideOrder = {"PATERNAL", "DIRECT", "MATERNAL", "SPOUSE_FAMILY", "STEP_FAMILY"};
        for (String side : sideOrder) {
            groups.put(side, new ArrayList<>());
        }

        for (FamilyNode node : nodes) {
            String side = node.relationshipSide != null ? node.relationshipSide : "DIRECT";
            groups.computeIfAbsent(side, k -> new ArrayList<>()).add(node);
            Log.d(TAG, "Grouped node " + node.name + " into side: " + side);
        }

        groups.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return groups;
    }

    private List<String> wrapText(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        if (paint.measureText(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = paint.measureText(testLine);
            if (testWidth <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private void logNodePositions() {
        Log.d(TAG, "=== NODE POSITIONS ===");
        if (rootNode != null) {
            Log.d(TAG, "ROOT: " + rootNode.name + " at (" + rootNode.x + ", " + rootNode.y + ")");
        }
        for (Map.Entry<Integer, List<FamilyNode>> entry : generationMap.entrySet()) {
            int level = entry.getKey();
            List<FamilyNode> nodes = entry.getValue();
            Log.d(TAG, "LEVEL " + level + ":");
            for (FamilyNode node : nodes) {
                Log.d(TAG, "  " + node.name + " (" + node.relationshipDisplayName + ") at (" + node.x + ", " + node.y + ")");
            }
        }
        Log.d(TAG, "======================");
    }

    private void centerOnRoot() {
        if (rootNode != null && getWidth() > 0 && getHeight() > 0) {
            translateX = getWidth() / 2f - rootNode.x * scaleFactor;
            translateY = getHeight() / 2f - rootNode.y * scaleFactor;
            Log.d(TAG, "Centered view: translateX=" + translateX + ", translateY=" + translateY + ", scale=" + scaleFactor);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        drawLevelIndicators(canvas);

        // CORRECTED: Draw connections based ONLY on database relationships
        drawDatabaseRelationshipConnections(canvas);

        drawNodes(canvas);
        canvas.restore();
    }

    private void drawLevelIndicators(Canvas canvas) {
        if (rootNode == null) return;

        List<Integer> sortedLevels = new ArrayList<>(generationMap.keySet());
        Collections.sort(sortedLevels);

        int leftMargin = 80;
        canvas.drawText("You", leftMargin, rootNode.y + 10, levelPaint);

        for (int level : sortedLevels) {
            List<FamilyNode> levelNodes = generationMap.get(level);
            if (levelNodes == null || levelNodes.isEmpty()) continue;

            String levelText = getLevelDisplayName(level);
            float yPosition;
            if (level == 0) {
                yPosition = rootNode.y + 30;
                levelText = "Same Generation";
            } else {
                yPosition = rootNode.y + (level * NODE_SPACING_Y) + 10;
            }
            canvas.drawText(levelText, leftMargin, yPosition, levelPaint);
        }
    }

    private String getLevelDisplayName(int level) {
        switch (level) {
            case -3: return "Great Grandparents";
            case -2: return "Grandparents";
            case -1: return "Parents";
            case 0: return "You";
            case 1: return "Children";
            case 2: return "Grandchildren";
            case 3: return "Great Grandchildren";
            default: return level < 0 ? "Generation " + Math.abs(level) + " Up" : "Generation " + level + " Down";
        }
    }

    /**
     * CORRECTED: Draw connections based ONLY on actual database relationships
     */
    private void drawDatabaseRelationshipConnections(Canvas canvas) {
        if (rootNode == null || databaseRelationships.isEmpty()) return;

        Log.d(TAG, "=== DRAWING DATABASE RELATIONSHIP CONNECTIONS ===");

        // Draw connections ONLY for relationships that exist in the database
        for (DatabaseRelationship dbRel : databaseRelationships) {
            FamilyNode fromNode = findNodeByUserId(dbRel.fromUserId);
            FamilyNode toNode = findNodeByUserId(dbRel.toUserId);

            if (fromNode != null && toNode != null) {
                drawStraightLine(canvas, fromNode, toNode);
                Log.d(TAG, "Drew database relationship connection: " + fromNode.name + " -> " + toNode.name + " (" + dbRel.relationshipType + ")");
            } else {
                Log.w(TAG, "Could not find nodes for relationship: " + dbRel.fromUserId + " -> " + dbRel.toUserId);
            }
        }

        Log.d(TAG, "=== DATABASE RELATIONSHIP CONNECTIONS DRAWN ===");
    }

    /**
     * Find a node by user ID
     */
    private FamilyNode findNodeByUserId(Long userId) {
        if (rootNode != null && rootNode.userId.equals(userId)) {
            return rootNode;
        }

        for (List<FamilyNode> levelNodes : generationMap.values()) {
            for (FamilyNode node : levelNodes) {
                if (node.userId.equals(userId)) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Draw a simple straight line between two nodes
     */
    private void drawStraightLine(Canvas canvas, FamilyNode from, FamilyNode to) {
        // Calculate connection points on the edge of the circles
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance == 0) return; // Same position

        // Normalize direction
        float dirX = dx / distance;
        float dirY = dy / distance;

        // Calculate start and end points on circle edges
        float startX = from.x + dirX * NODE_RADIUS;
        float startY = from.y + dirY * NODE_RADIUS;
        float endX = to.x - dirX * NODE_RADIUS;
        float endY = to.y - dirY * NODE_RADIUS;

        // Draw the line
        canvas.drawLine(startX, startY, endX, endY, linePaint);
    }

    private void drawNodes(Canvas canvas) {
        // Draw non-root nodes first
        for (List<FamilyNode> levelNodes : generationMap.values()) {
            for (FamilyNode node : levelNodes) {
                drawNode(canvas, node);
            }
        }

        // Draw root node last (on top)
        if (rootNode != null) {
            drawNode(canvas, rootNode);
        }
    }

    private void drawNode(Canvas canvas, FamilyNode node) {
        Paint backgroundPaint = node.isRoot ? rootNodePaint : nodePaint;
        Paint borderPaint = node.isRoot ? rootNodeBorderPaint : nodeBorderPaint;

        canvas.drawCircle(node.x, node.y, NODE_RADIUS, backgroundPaint);
        canvas.drawCircle(node.x, node.y, NODE_RADIUS, borderPaint);

        if (node.profileBitmap != null) {
            float imageSize = NODE_RADIUS - PROFILE_MARGIN;
            Rect destRect = new Rect(
                    (int) (node.x - imageSize),
                    (int) (node.y - imageSize),
                    (int) (node.x + imageSize),
                    (int) (node.y + imageSize)
            );
            canvas.drawBitmap(node.profileBitmap, null, destRect, null);
        }

        drawEnhancedNodeText(canvas, node);
    }

    private void drawEnhancedNodeText(Canvas canvas, FamilyNode node) {
        // Draw name with multi-line support
        String fullName = node.name != null ? node.name : "";
        List<String> nameLines = wrapText(fullName, textPaint, MAX_TEXT_WIDTH);
        float nameStartY = node.y + NODE_RADIUS + NAME_MARGIN;
        float lineHeight = textPaint.getTextSize() + TEXT_LINE_SPACING;

        // Calculate total name height
        float totalNameHeight = nameLines.size() * lineHeight;

        // Draw background for all name lines
        if (!nameLines.isEmpty()) {
            float maxWidth = 0;
            for (String line : nameLines) {
                maxWidth = Math.max(maxWidth, textPaint.measureText(line));
            }
            drawMultiLineTextBackground(canvas, nameLines, node.x, nameStartY, lineHeight, maxWidth, textPaint);
        }

        // Draw each line of the name
        for (int i = 0; i < nameLines.size(); i++) {
            float lineY = nameStartY + (i * lineHeight);
            canvas.drawText(nameLines.get(i), node.x, lineY, textPaint);
        }

        // Draw relationship text with proper spacing
        if (!node.isRoot && node.relationshipDisplayName != null && !node.relationshipDisplayName.isEmpty()) {
            List<String> relationshipLines = wrapText(node.relationshipDisplayName, relationshipTextPaint, MAX_TEXT_WIDTH);
            float relationshipStartY = nameStartY + totalNameHeight + 20;
            float relationshipLineHeight = relationshipTextPaint.getTextSize() + TEXT_LINE_SPACING;

            // Draw background for relationship lines
            if (!relationshipLines.isEmpty()) {
                float maxWidth = 0;
                for (String line : relationshipLines) {
                    maxWidth = Math.max(maxWidth, relationshipTextPaint.measureText(line));
                }
                drawMultiLineTextBackground(canvas, relationshipLines, node.x, relationshipStartY, relationshipLineHeight, maxWidth, relationshipTextPaint);
            }

            // Draw each line of the relationship
            for (int i = 0; i < relationshipLines.size(); i++) {
                float lineY = relationshipStartY + (i * relationshipLineHeight);
                canvas.drawText(relationshipLines.get(i), node.x, lineY, relationshipTextPaint);
            }
        }
    }

    private void drawMultiLineTextBackground(Canvas canvas, List<String> lines, float centerX, float startY, float lineHeight, float maxWidth, Paint textPaint) {
        if (lines.isEmpty()) return;

        float padding = TEXT_PADDING;
        float totalHeight = lines.size() * lineHeight;
        RectF backgroundRect = new RectF(
                centerX - maxWidth/2 - padding,
                startY - lineHeight/2 - padding,
                centerX + maxWidth/2 + padding,
                startY + totalHeight - lineHeight/2 + padding
        );
        canvas.drawRoundRect(backgroundRect, 12, 12, textBackgroundPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        if (!scaleGestureDetector.isInProgress()) {
            gestureDetector.onTouchEvent(event);
        }
        return true;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float canvasX = (e.getX() - translateX) / scaleFactor;
            float canvasY = (e.getY() - translateY) / scaleFactor;
            FamilyNode touchedNode = findNodeAt(canvasX, canvasY);
            if (touchedNode != null && nodeClickListener != null) {
                nodeClickListener.onNodeClick(touchedNode);
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            float canvasX = (e.getX() - translateX) / scaleFactor;
            float canvasY = (e.getY() - translateY) / scaleFactor;
            FamilyNode touchedNode = findNodeAt(canvasX, canvasY);
            if (touchedNode != null && nodeLongClickListener != null) {
                nodeLongClickListener.onNodeLongClick(touchedNode);
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            translateX -= distanceX;
            translateY -= distanceY;
            invalidate();
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.3f, Math.min(scaleFactor, 5.0f));
            invalidate();
            return true;
        }
    }

    private FamilyNode findNodeAt(float x, float y) {
        if (rootNode != null && isPointInNode(x, y, rootNode)) {
            return rootNode;
        }
        for (List<FamilyNode> levelNodes : generationMap.values()) {
            for (FamilyNode node : levelNodes) {
                if (isPointInNode(x, y, node)) {
                    return node;
                }
            }
        }
        return null;
    }

    private boolean isPointInNode(float x, float y, FamilyNode node) {
        float distance = (float) Math.sqrt(Math.pow(x - node.x, 2) + Math.pow(y - node.y, 2));
        return distance <= NODE_RADIUS;
    }

    // NEW: Class to represent actual database relationships
    public static class DatabaseRelationship {
        public Long fromUserId;
        public Long toUserId;
        public String relationshipType;
        public int generationLevel;
    }

    public static class FamilyNode {
        public Long userId;
        public String name;
        public String email;
        public String relationshipDisplayName;
        public int generationLevel;
        public String generationName;
        public String relationshipSide;
        public boolean isRoot = false;
        public Bitmap profileBitmap;
        public float x;
        public float y;
    }
}
