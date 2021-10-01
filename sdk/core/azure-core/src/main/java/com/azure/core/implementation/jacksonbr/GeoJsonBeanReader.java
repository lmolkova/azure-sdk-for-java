// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.models.GeoBoundingBox;
import com.azure.core.models.GeoCollection;
import com.azure.core.models.GeoLineString;
import com.azure.core.models.GeoLineStringCollection;
import com.azure.core.models.GeoLinearRing;
import com.azure.core.models.GeoObject;
import com.azure.core.models.GeoObjectType;
import com.azure.core.models.GeoPoint;
import com.azure.core.models.GeoPointCollection;
import com.azure.core.models.GeoPolygon;
import com.azure.core.models.GeoPolygonCollection;
import com.azure.core.models.GeoPosition;
import com.azure.core.util.logging.ClientLogger;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class GeoJsonBeanReader<T extends GeoObject> extends ValueReader<T> {
    private static final ClientLogger LOGGER = new ClientLogger(GeoJsonBeanReader.class);

    /*
     * Required GeoJSON properties.
     */
    static final String TYPE_PROPERTY = "type";
    static final String GEOMETRIES_PROPERTY = "geometries";
    static final String COORDINATES_PROPERTY = "coordinates";

    /*
     * Optional GeoJSON properties.
     */
    static final String BOUNDING_BOX_PROPERTY = "bbox";

    public GeoJsonBeanReader(Class<T> type) {
        super(type);
    }


    @Override
    public T read(JsonParser parser) throws IOException {
        return read((TreeNode) parser.readValueAsTree());
    }

    /*private static TreeNode getRequiredProperty(TreeNode node, String name) {
        TreeNode requiredNode = node.get(name);

        if (requiredNode == null) {
            throw LOGGER.logExceptionAsError(new IllegalStateException(
                String.format("GeoJSON object expected to have '%s' property.", name)));
        }

        return requiredNode;
    }*/

    private T read(TreeNode node) {
        return null;
        /*String type = getRequiredProperty(node, TYPE_PROPERTY);

        if (isGeoObjectType(type, GeoObjectType.GEOMETRY_COLLECTION)) {
            List<GeoObject> geometries = new ArrayList<>();
            for (TreeNode geoNode : getRequiredProperty(node, GEOMETRIES_PROPERTY)) {
                geometries.add(read(geoNode));
            }

            return new GeoCollection(geometries, readBoundingBox(node),
                readProperties(node, GEOMETRIES_PROPERTY));
        }

        TreeNode coordinates = getRequiredProperty(node, COORDINATES_PROPERTY);

        GeoBoundingBox boundingBox = readBoundingBox(node);
        Map<String, Object> properties = readProperties(node);

        if (isGeoObjectType(type, GeoObjectType.POINT)) {
            return new GeoPoint(readCoordinate(coordinates), boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.LINE_STRING)) {
            return new GeoLineString(readCoordinates(coordinates), boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.POLYGON)) {
            List<GeoLinearRing> rings = new ArrayList<>();
            coordinates.forEach(ring -> rings.add(new GeoLinearRing(readCoordinates(ring))));

            return new GeoPolygon(rings, boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.MULTI_POINT)) {
            List<GeoPoint> points = new ArrayList<>();
            readCoordinates(coordinates).forEach(position -> points.add(new GeoPoint(position)));

            return new GeoPointCollection(points, boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.MULTI_LINE_STRING)) {
            List<GeoLineString> lines = new ArrayList<>();
            coordinates.forEach(line -> lines.add(new GeoLineString(readCoordinates(line))));

            return new GeoLineStringCollection(lines, boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.MULTI_POLYGON)) {
            return readMultiPolygon(coordinates, boundingBox, properties);
        }*/

        //throw LOGGER.logExceptionAsError(new IllegalStateException(String.format("Unsupported geo type %s.", this._valueType)));
    }

    private static boolean isGeoObjectType(String jsonType, GeoObjectType type) {
        return type.toString().equalsIgnoreCase(jsonType);
    }

    /*private static GeoPolygonCollection readMultiPolygon(TreeNode node, GeoBoundingBox boundingBox,
        Map<String, Object> properties) {
        List<GeoPolygon> polygons = new ArrayList<>();
        for (TreeNode polygon : node) {
            List<GeoLinearRing> rings = new ArrayList<>();
            polygon.forEach(ring -> rings.add(new GeoLinearRing(readCoordinates(ring))));

            polygons.add(new GeoPolygon(rings));
        }

        return new GeoPolygonCollection(polygons, boundingBox, properties);
    }



    private static GeoBoundingBox readBoundingBox(TreeNode node) {
        TreeNode boundingBoxNode = node.get(BOUNDING_BOX_PROPERTY);
        if (boundingBoxNode != null) {
            switch (boundingBoxNode.size()) {
                case 4:
                    return new GeoBoundingBox(boundingBoxNode.get(0).asDouble(),
                        boundingBoxNode.get(1).asDouble(), boundingBoxNode.get(2).asDouble(),
                        boundingBoxNode.get(3).asDouble());
                case 6:
                    return new GeoBoundingBox(boundingBoxNode.get(0).asDouble(),
                        boundingBoxNode.get(1).asDouble(), boundingBoxNode.get(3).asDouble(),
                        boundingBoxNode.get(4).asDouble(), boundingBoxNode.get(2).asDouble(),
                        boundingBoxNode.get(5).asDouble());
                default:
                    throw LOGGER.logExceptionAsError(
                        new IllegalStateException("Only 2 or 3 dimension bounding boxes are supported."));
            }
        }

        return null;
    }

    private static Map<String, Object> readProperties(TreeNode node) {
        return readProperties(node, COORDINATES_PROPERTY);
    }

    private static Map<String, Object> readProperties(TreeNode node, String knownProperty) {
        Map<String, Object> additionalProperties = null;
        Iterator<Map.Entry<String, TreeNode>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, TreeNode> field = fieldsIterator.next();
            String propertyName = field.getKey();
            if (propertyName.equalsIgnoreCase(TYPE_PROPERTY)
                || propertyName.equalsIgnoreCase(BOUNDING_BOX_PROPERTY)
                || propertyName.equalsIgnoreCase(knownProperty)) {
                continue;
            }

            if (additionalProperties == null) {
                additionalProperties = new HashMap<>();
            }

            additionalProperties.put(propertyName, readAdditionalPropertyValue(field.getValue()));
        }

        return additionalProperties;
    }

    private static Object readAdditionalPropertyValue(TreeNode node) {
        switch (node.getNodeType()) {
            case STRING:
                return node.asText();
            case NUMBER:
                if (node.isInt()) {
                    return node.asInt();
                } else if (node.isLong()) {
                    return node.asLong();
                } else if (node.isFloat()) {
                    return node.floatValue();
                } else {
                    return node.asDouble();
                }
            case BOOLEAN:
                return node.asBoolean();
            case NULL:
            case MISSING:
                return null;
            case OBJECT:
                Map<String, Object> object = new HashMap<>();
                node.fields().forEachRemaining(field ->
                    object.put(field.getKey(), readAdditionalPropertyValue(field.getValue())));

                return object;
            case ARRAY:
                List<Object> array = new ArrayList<>();
                node.forEach(element -> array.add(readAdditionalPropertyValue(element)));

                return array;
            default:
                throw LOGGER.logExceptionAsError(new IllegalStateException(
                    String.format("Unsupported additional property type %s.", node.getNodeType())));
        }
    }

    private static List<GeoPosition> readCoordinates(TreeNode coordinates) {
        List<GeoPosition> positions = new ArrayList<>();

        coordinates.forEach(coordinate -> positions.add(readCoordinate(coordinate)));

        return positions;
    }

    private static GeoPosition readCoordinate(TreeNode coordinate) {
        int coordinateCount = coordinate.size();

        if (coordinateCount < 2 || coordinateCount > 3) {
            throw LOGGER.logExceptionAsError(new IllegalStateException("Only 2 or 3 element coordinates supported."));
        }

        double longitude = coordinate.get(0).asDouble();
        double latitude = coordinate.get(1).asDouble();
        Double altitude = null;

        if (coordinateCount > 2) {
            altitude = coordinate.get(2).asDouble();
        }

        return new GeoPosition(longitude, latitude, altitude);
    }

    private static <T extends GeoObject> ValueReader<T> geoSubclassDeserializer(Class<T> subclass) {
        return new ValueReader<T>(subclass) {
            @Override
            public T read(JsonParser p) throws IOException {
                return subclass.cast(p.readValueAsTree());
            }
        };
    }*/


}
