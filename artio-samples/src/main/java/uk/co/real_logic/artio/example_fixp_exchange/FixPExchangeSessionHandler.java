package uk.co.real_logic.artio.example_fixp_exchange;

import b3.entrypoint.fixp.sbe.*;
import org.agrona.DirectBuffer;
import uk.co.real_logic.artio.binary_entrypoint.BinaryEntryPointConnection;
import uk.co.real_logic.artio.fixp.FixPConnection;
import uk.co.real_logic.artio.fixp.FixPConnectionHandler;
import uk.co.real_logic.artio.library.NotAppliedResponse;
import uk.co.real_logic.artio.messages.DisconnectReason;

public class FixPExchangeSessionHandler implements FixPConnectionHandler
{
    private final NewOrderSingleDecoder newOrderSingle = new NewOrderSingleDecoder();
    private final ExecutionReport_NewEncoder executionReport = new ExecutionReport_NewEncoder();

    private long orderId = ExecutionReport_NewEncoder.orderIDMinValue();

    FixPExchangeSessionHandler(final BinaryEntryPointConnection connection)
    {
    }

    public void onBusinessMessage(
        final FixPConnection connection,
        final int templateId,
        final DirectBuffer buffer,
        final int offset,
        final int blockLength,
        final int version,
        final boolean possRetrans)
    {
        System.out.println("Received Business Message");
        if (templateId == NewOrderSingleDecoder.TEMPLATE_ID)
        {
            System.out.println("Type=NewOrderSingle");
            final NewOrderSingleDecoder newOrderSingle = this.newOrderSingle;
            final ExecutionReport_NewEncoder executionReport = this.executionReport;

            newOrderSingle.wrap(buffer, offset, blockLength, version);


            final long position = connection.tryClaim(
                executionReport, ExecutionReport_NewEncoder.NoMetricsEncoder.sbeBlockLength());

            if (position < 0)
            {
                // TODO: back-pressure
            }

            executionReport
                .orderID(orderId++)
                .clOrdID(newOrderSingle.clOrdID())
                .securityID(newOrderSingle.securityID())
                .secondaryOrderID(ExecutionReport_NewEncoder.secondaryOrderIDNullValue())
                .ordStatus(OrdStatus.NEW)
                .execRestatementReason(ExecRestatementReason.NULL_VAL)
                .multiLegReportingType(MultiLegReportingType.NULL_VAL)
                .workingIndicator(Bool.NULL_VAL)
                .transactTime().time(System.nanoTime());
            executionReport
                .putTradeDate(1, 2)
                .protectionPrice().mantissa(1234);
            executionReport.receivedTime().time(System.nanoTime());
            executionReport.noMetricsCount(0);

            connection.commit();
            System.out.println("Sent Execution Report New");
        }
    }

    public void onNotApplied(
        final FixPConnection connection,
        final long fromSequenceNumber,
        final long msgCount,
        final NotAppliedResponse response)
    {
    }

    public void onRetransmitReject(
        final FixPConnection connection, final String reason, final long requestTimestamp, final int errorCodes)
    {
    }

    public void onRetransmitTimeout(final FixPConnection connection)
    {
    }

    public void onSequence(final FixPConnection connection, final long nextSeqNo)
    {
    }

    public void onError(final FixPConnection connection, final Exception ex)
    {
        ex.printStackTrace();
    }

    public void onDisconnect(final FixPConnection connection, final DisconnectReason reason)
    {
        System.out.println("onDisconnect conn=" + connection.key() + ",reason=" + reason);
    }
}
