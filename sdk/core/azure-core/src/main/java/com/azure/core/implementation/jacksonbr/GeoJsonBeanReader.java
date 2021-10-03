// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.jacksonbr.tree.JacksonJrsTreeCodec;
import com.azure.core.implementation.jacksonbr.tree.JrsArray;
import com.azure.core.implementation.jacksonbr.tree.JrsBoolean;
import com.azure.core.implementation.jacksonbr.tree.JrsMissing;
import com.azure.core.implementation.jacksonbr.tree.JrsNull;
import com.azure.core.implementation.jacksonbr.tree.JrsNumber;
import com.azure.core.implementation.jacksonbr.tree.JrsObject;
import com.azure.core.implementation.jacksonbr.tree.JrsString;
import com.azure.core.implementation.jacksonbr.tree.JrsValue;
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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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

        return (T)readNode(JacksonJrsTreeCodec.SINGLETON.readTree(parser));
    }

    private static TreeNode getRequiredProperty(TreeNode node, String name) {
        TreeNode requiredNode = node.get(name);

        if (requiredNode == null) {
            throw LOGGER.logExceptionAsError(new IllegalStateException(
                String.format("GeoJSON object expected to have '%s' property.", name)));
        }

        return requiredNode;
    }

    private <R> R getRequiredProperty(TreeNode node, String name, Class<R> as) {
        TreeNode propNode = getRequiredProperty(node, name);
        if (as.isInstance(propNode)) {
            return (R)propNode;
        }

        return null;
    }

    private GeoObject readNode(TreeNode node) {
        String type = getRequiredProperty(node, TYPE_PROPERTY, JrsValue.class).asText();

        if (isGeoObjectType(type, GeoObjectType.GEOMETRY_COLLECTION)) {
            List<GeoObject> geometries = new ArrayList<>();
            TreeNode geometriesNode = getRequiredProperty(node, GEOMETRIES_PROPERTY);
            if (geometriesNode != null && geometriesNode.isArray()) {
                Iterator<JrsValue> it = ((JrsArray)geometriesNode).elements();
                while (it.hasNext()) {
                    geometries.add(readNode(it.next()));
                }
            }

            return new GeoCollection(geometries, readBoundingBox(node),
                readProperties((JrsObject)node, GEOMETRIES_PROPERTY));
        }

        JrsArray coordinatesNode = getRequiredProperty(node, COORDINATES_PROPERTY, JrsArray.class);

        GeoBoundingBox boundingBox = readBoundingBox(node);
        Map<String, Object> properties = readProperties((JrsObject)node);

        if (isGeoObjectType(type, GeoObjectType.POINT)) {
            return new GeoPoint(readCoordinate(coordinatesNode), boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.LINE_STRING)) {
            return new GeoLineString(readCoordinates(coordinatesNode), boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.POLYGON)) {
            List<GeoLinearRing> rings = new ArrayList<>();
            Iterator<JrsValue> it = coordinatesNode.elements();
            while (it.hasNext()) {
                rings.add(new GeoLinearRing(readCoordinates((JrsArray) it.next())));
            }
            return new GeoPolygon(rings, boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.MULTI_POINT)) {
            List<GeoPoint> points = new ArrayList<>();
            readCoordinates(coordinatesNode).forEach(position -> points.add(new GeoPoint(position)));

            return new GeoPointCollection(points, boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.MULTI_LINE_STRING)) {
            List<GeoLineString> lines = new ArrayList<>();
            Iterator<JrsValue> it = coordinatesNode.elements();
            while (it.hasNext()) {
                lines.add(new GeoLineString(readCoordinates((JrsArray) it.next())));
            }

            return new GeoLineStringCollection(lines, boundingBox, properties);
        } else if (isGeoObjectType(type, GeoObjectType.MULTI_POLYGON)) {
            return readMultiPolygon(coordinatesNode, boundingBox, properties);
        }

        throw LOGGER.logExceptionAsError(new IllegalStateException(String.format("Unsupported geo type %s.", this._valueType)));
    }

    private static boolean isGeoObjectType(String jsonType, GeoObjectType type) {
        return type.toString().equalsIgnoreCase(jsonType);
    }

    private static GeoPolygonCollection readMultiPolygon(JrsArray node, GeoBoundingBox boundingBox,
        Map<String, Object> properties) {
        List<GeoPolygon> polygons = new ArrayList<>();

        Iterator<JrsValue> it = node.elements();
        while (it.hasNext()) {
            JrsArray polygon = (JrsArray) it.next();
            Iterator<JrsValue> pit = polygon.elements();
            List<GeoLinearRing> rings = new ArrayList<>();
            while (pit.hasNext()) {
                rings.add(new GeoLinearRing(readCoordinates((JrsArray)pit.next())));
            }

            polygons.add(new GeoPolygon(rings));
        }

        return new GeoPolygonCollection(polygons, boundingBox, properties);
    }


    private static GeoBoundingBox readBoundingBox(TreeNode node) {
        TreeNode boundingBoxNode = node.get(BOUNDING_BOX_PROPERTY);
        if (boundingBoxNode != null) {
            switch (boundingBoxNode.size()) {
                case 4:
                    return new GeoBoundingBox(((JrsNumber)boundingBoxNode.get(0)).getValue().doubleValue(),
                        ((JrsNumber)boundingBoxNode.get(1)).getValue().doubleValue(), ((JrsNumber)boundingBoxNode.get(2)).getValue().doubleValue(),
                        ((JrsNumber)boundingBoxNode.get(3)).getValue().doubleValue());
                case 6:
                    return new GeoBoundingBox(
                        ((JrsNumber)boundingBoxNode.get(0)).getValue().doubleValue(),
                        ((JrsNumber)boundingBoxNode.get(1)).getValue().doubleValue(), ((JrsNumber)boundingBoxNode.get(3)).getValue().doubleValue(),
                        ((JrsNumber)boundingBoxNode.get(4)).getValue().doubleValue(), ((JrsNumber)boundingBoxNode.get(2)).getValue().doubleValue(),
                        ((JrsNumber)boundingBoxNode.get(5)).getValue().doubleValue());
                default:
                    throw LOGGER.logExceptionAsError(
                        new IllegalStateException("Only 2 or 3 dimension bounding boxes are supported."));
            }
        }

        return null;
    }

    private static Map<String, Object> readProperties(JrsObject node) {
        return readProperties(node, COORDINATES_PROPERTY);
    }

    private static Map<String, Object> readProperties(JrsObject node, String knownProperty) {
        Map<String, Object> additionalProperties = null;
        Iterator<Map.Entry<String, JrsValue>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JrsValue> field = fieldsIterator.next();
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

    private static Object readAdditionalPropertyValue(JrsValue node) {
        if (node instanceof JrsString)  {
            return node.asText();
        }
        if (node instanceof JrsNumber) {
            return ((JrsNumber) node).getValue();
        }

        if (node instanceof JrsBoolean) {
            return ((JrsBoolean)node).booleanValue();
        }

        if (node instanceof JrsNull || node instanceof JrsMissing) {
            return null;
        }

        if (node instanceof JrsObject) {
            Map<String, Object> object = new HashMap<>();

            ((JrsObject) node).fields().forEachRemaining(field ->
                object.put(field.getKey(), readAdditionalPropertyValue(field.getValue())));

            return object;
        }
        if (node instanceof JrsArray) {
            List<Object> array = new ArrayList<>();
            Iterator<JrsValue> it = ((JrsArray) node).elements();
            while (it.hasNext()) {
                array.add(readAdditionalPropertyValue(it.next()));
            }
            return array;
        }

        throw LOGGER.logExceptionAsError(new IllegalStateException(
            String.format("Unsupported additional property type %s.", node.asToken().id())));
    }

    private static List<GeoPosition> readCoordinates(JrsArray coordinates) {
        List<GeoPosition> positions = new ArrayList<>();

        Iterator<JrsValue> it  = coordinates.elements();
        while (it.hasNext()) {
            positions.add(readCoordinate((JrsArray)it.next()));
        }
        return positions;
    }

    private static GeoPosition readCoordinate(JrsArray coordinate) {
        int coordinateCount = coordinate.size();

        if (coordinateCount < 2 || coordinateCount > 3) {
            throw LOGGER.logExceptionAsError(new IllegalStateException("Only 2 or 3 element coordinates supported."));
        }

        double longitude = (Double) ((JrsNumber)coordinate.get(0)).getValue();
        double latitude = (Double) ((JrsNumber)coordinate.get(1)).getValue();
        Double altitude = null;

        if (coordinateCount > 2) {
            altitude = (Double) ((JrsNumber)coordinate.get(2)).getValue();
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
    }
}
